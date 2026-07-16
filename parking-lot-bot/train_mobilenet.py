#!/usr/bin/env python
# coding: utf-8

# # Parking Lot Occupancy with MobileNetV3-Small
# 
# This notebook downloads PKLot, extracts each annotated parking space, trains an occupied/empty classifier, evaluates it, and overlays predictions on a complete parking-lot image.

# In[4]:


from __future__ import annotations

import random
import json
import os
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path

import cv2
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import torch
from PIL import Image
from sklearn.model_selection import GroupShuffleSplit
from torch import nn
from torch.utils.data import DataLoader, Dataset
from torchvision import transforms
from torchvision.models import MobileNet_V3_Small_Weights, mobilenet_v3_small
from tqdm.auto import tqdm

SEED = 27
random.seed(SEED); np.random.seed(SEED); torch.manual_seed(SEED)
DEVICE = torch.device('cuda' if torch.cuda.is_available() else ('mps' if torch.backends.mps.is_available() else 'cpu'))
print(f'Training device: {DEVICE}')


# ## Configuration
# Lower `MAX_SAMPLES_PER_CLASS` for a quick rehearsal; use `None` for the complete dataset.

# In[5]:


PROJECT_ROOT = Path.cwd().parent if Path.cwd().name == 'parking-lot-bot' else Path.cwd()
DATA_DIR = PROJECT_ROOT / 'data' / 'pklot'
MODEL_PATH = PROJECT_ROOT / 'models' / 'mobilenet_v3_small_pklot.pt'
KAGGLE_DATASET = 'ammarnassanalhajali/pklot-dataset'
IMAGE_SIZE = 160
BATCH_SIZE = 64
# Demo-sized balanced subset: 5,000 train, 1,000 validation, 1,000 test.
MAX_SAMPLES_PER_CLASS = 2_500
MAX_EVAL_SAMPLES_PER_CLASS = 500
FROZEN_EPOCHS = 3
FINE_TUNE_EPOCHS = 10
PATIENCE = 3
NUM_WORKERS = 0  # safest inside Jupyter; try 2-4 on Linux
DATA_DIR, MODEL_PATH


# In[6]:


has_annotations = DATA_DIR.exists() and (
    any(DATA_DIR.rglob('*.xml')) or any(DATA_DIR.rglob('*_annotations.coco.json'))
)
if not has_annotations:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    kaggle_executable = Path(sys.executable).with_name('kaggle')
    if not kaggle_executable.exists():
        raise RuntimeError(f'Kaggle CLI not found at {kaggle_executable}. Install requirements.txt first.')
    subprocess.run([str(kaggle_executable), 'datasets', 'download', '-d', KAGGLE_DATASET, '-p', str(DATA_DIR), '--unzip'], check=True)
print(f'Dataset directory: {DATA_DIR}')


# ## Read PKLot annotations
# Both original PKLot rotated rectangles and converted Pascal VOC boxes are supported.

# In[ ]:


@dataclass(frozen=True)
class Spot:
    image_path: Path
    annotation_path: Path
    label: int  # 0 empty, 1 occupied
    group: str
    kind: str
    geometry: tuple[float, ...]
    split: str | None = None

def image_for_xml(xml_path: Path) -> Path | None:
    for suffix in ('.jpg', '.jpeg', '.png', '.JPG', '.JPEG', '.PNG'):
        candidate = xml_path.with_suffix(suffix)
        if candidate.exists(): return candidate
    nearby = [p for p in xml_path.parent.rglob(f'{xml_path.stem}.*') if p.suffix.lower() in {'.jpg', '.jpeg', '.png'}]
    return nearby[0] if len(nearby) == 1 else None

def voc_label(name: str) -> int | None:
    name = name.strip().lower().replace('-', '_').replace(' ', '_')
    if name in {'occupied', 'not_free', 'not_free_parking_space', '1'}: return 1
    if name in {'empty', 'free', 'free_parking_space', 'vacant', '0'}: return 0
    return None

def parse_spots(xml_path: Path) -> list[Spot]:
    image_path = image_for_xml(xml_path)
    if image_path is None: return []
    root = ET.parse(xml_path).getroot()
    group = str(image_path.parent.relative_to(DATA_DIR))
    spots = []
    for space in root.findall('.//space'):
        value = space.get('occupied')
        center, size, angle = space.find('.//center'), space.find('.//size'), space.find('.//angle')
        if value in {'0', '1'} and center is not None and size is not None and angle is not None:
            geometry = (float(center.get('x')), float(center.get('y')), float(size.get('w')), float(size.get('h')), float(angle.get('d')))
            spots.append(Spot(image_path, xml_path, int(value), group, 'rotated', geometry, None))
    for obj in root.findall('.//object'):
        label = voc_label(obj.findtext('name', ''))
        box = obj.find('bndbox')
        if label is not None and box is not None:
            geometry = tuple(float(box.findtext(key)) for key in ('xmin', 'ymin', 'xmax', 'ymax'))
            spots.append(Spot(image_path, xml_path, label, group, 'box', geometry, None))
    return spots

def parse_coco(coco_path: Path, max_per_class: int | None) -> list[Spot]:
    with coco_path.open(encoding='utf-8') as file:
        data = json.load(file)
    images = {image['id']: image for image in data['images']}
    categories = {category['id']: category['name'].lower() for category in data['categories']}
    split = coco_path.parent.name.lower().replace('validation', 'valid')
    usable_annotations = [annotation for annotation in data['annotations'] if categories.get(annotation['category_id'], '') in {'space-empty', 'space-occupied'}]
    if max_per_class is not None:
        rng = random.Random(f'{SEED}-{split}')
        selected = []
        for category_id in (1, 2):
            matching = [annotation for annotation in usable_annotations if annotation['category_id'] == category_id]
            selected.extend(rng.sample(matching, min(max_per_class, len(matching))))
        usable_annotations = selected
    spots = []
    for annotation in usable_annotations:
        category = categories.get(annotation['category_id'], '')
        if category == 'space-empty': label = 0
        elif category == 'space-occupied': label = 1
        else: continue
        image_info = images[annotation['image_id']]
        image_path = coco_path.parent / image_info['file_name']
        if not image_path.exists(): continue
        x, y, width, height = annotation['bbox']
        geometry = (x, y, x + width, y + height)
        capture_day = image_info['file_name'][:10]
        spots.append(Spot(image_path, coco_path, label, capture_day, 'box', geometry, split))
    return spots

coco_files = list(DATA_DIR.rglob('*_annotations.coco.json'))
records = []
if coco_files:
    for coco_path in tqdm(coco_files, desc='Reading COCO annotations'):
        cap = MAX_SAMPLES_PER_CLASS if coco_path.parent.name.lower() == 'train' else MAX_EVAL_SAMPLES_PER_CLASS
        records.extend(parse_coco(coco_path, cap))
else:
    for xml_path in tqdm(list(DATA_DIR.rglob('*.xml')), desc='Reading XML annotations'):
        try: records.extend(parse_spots(xml_path))
        except (ET.ParseError, TypeError, ValueError): pass
assert records, 'No usable COCO JSON or XML annotations found. Check DATA_DIR.'
pd.Series([r.label for r in records]).value_counts().rename(index={0: 'empty', 1: 'occupied'})


# In[ ]:


all_records = records
print(f'Found {len(all_records):,} parking-space crops')


# In[ ]:


def crop_spot(spot: Spot) -> Image.Image:
    image = cv2.imread(str(spot.image_path))
    if image is None: raise FileNotFoundError(spot.image_path)
    if spot.kind == 'box':
        x1, y1, x2, y2 = map(round, spot.geometry)
        crop = image[max(0, y1):y2, max(0, x1):x2]
    else:
        cx, cy, width, height, angle = spot.geometry
        matrix = cv2.getRotationMatrix2D((cx, cy), angle, 1.0)
        rotated = cv2.warpAffine(image, matrix, (image.shape[1], image.shape[0]))
        crop = cv2.getRectSubPix(rotated, (max(1, round(width)), max(1, round(height))), (cx, cy))
    if crop.size == 0: raise ValueError(f'Empty crop for {spot.image_path}')
    return Image.fromarray(cv2.cvtColor(crop, cv2.COLOR_BGR2RGB))

fig, axes = plt.subplots(2, 5, figsize=(12, 5))
for axis, spot in zip(axes.flat, random.sample(records, 10)):
    axis.imshow(crop_spot(spot)); axis.set_title('occupied' if spot.label else 'empty'); axis.axis('off')
plt.tight_layout()


# ## Grouped train/validation/test split
# Grouping by source directory keeps images from the same capture session together and reduces temporal leakage.

# In[ ]:


def grouped_split(items: list[Spot]):
    groups = np.array([item.group for item in items])
    first = GroupShuffleSplit(n_splits=1, test_size=0.30, random_state=SEED)
    train_idx, holdout_idx = next(first.split(items, groups=groups))
    holdout = [items[i] for i in holdout_idx]
    holdout_groups = np.array([item.group for item in holdout])
    second = GroupShuffleSplit(n_splits=1, test_size=0.50, random_state=SEED)
    val_idx, test_idx = next(second.split(holdout, groups=holdout_groups))
    return [items[i] for i in train_idx], [holdout[i] for i in val_idx], [holdout[i] for i in test_idx]

available_splits = {item.split for item in records}
if {'train', 'valid', 'test'} <= available_splits:
    train_records = [item for item in records if item.split == 'train']
    val_records = [item for item in records if item.split == 'valid']
    test_records = [item for item in records if item.split == 'test']
else:
    train_records, val_records, test_records = grouped_split(records)

if MAX_SAMPLES_PER_CLASS is not None and not coco_files:
    rng = random.Random(SEED)
    by_label = {label: [r for r in train_records if r.label == label] for label in (0, 1)}
    train_records = sum((rng.sample(items, min(MAX_SAMPLES_PER_CLASS, len(items))) for items in by_label.values()), [])
    rng.shuffle(train_records)
len(train_records), len(val_records), len(test_records)


# In[ ]:


train_transform = transforms.Compose([
    transforms.Resize((IMAGE_SIZE, IMAGE_SIZE)),
    transforms.RandomHorizontalFlip(),
    transforms.ColorJitter(brightness=.2, contrast=.2, saturation=.15),
    transforms.ToTensor(),
    transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225]),
])
eval_transform = transforms.Compose([
    transforms.Resize((IMAGE_SIZE, IMAGE_SIZE)), transforms.ToTensor(),
    transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225]),
])

class ParkingSpotDataset(Dataset):
    def __init__(self, items, transform): self.items, self.transform = items, transform
    def __len__(self): return len(self.items)
    def __getitem__(self, index):
        spot = self.items[index]
        return self.transform(crop_spot(spot)), spot.label

train_loader = DataLoader(ParkingSpotDataset(train_records, train_transform), batch_size=BATCH_SIZE, shuffle=True, num_workers=NUM_WORKERS)
val_loader = DataLoader(ParkingSpotDataset(val_records, eval_transform), batch_size=BATCH_SIZE, num_workers=NUM_WORKERS)
test_loader = DataLoader(ParkingSpotDataset(test_records, eval_transform), batch_size=BATCH_SIZE, num_workers=NUM_WORKERS)

if os.environ.get('PARKING_BOT_PREPARE_ONLY') == '1':
    print('Data preparation smoke test passed.')
    raise SystemExit(0)


# ## Transfer learning
# First train only the new classifier, then unfreeze the network with a smaller learning rate.

# In[ ]:


model = mobilenet_v3_small(weights=MobileNet_V3_Small_Weights.DEFAULT)
model.classifier[-1] = nn.Linear(model.classifier[-1].in_features, 2)
model = model.to(DEVICE)
criterion = nn.CrossEntropyLoss()
MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)

def run_epoch(loader, optimizer=None):
    training = optimizer is not None
    model.train(training)
    total_loss = total_correct = total = 0
    for images, labels in tqdm(loader, leave=False):
        images, labels = images.to(DEVICE), labels.to(DEVICE)
        with torch.set_grad_enabled(training):
            logits = model(images); loss = criterion(logits, labels)
            if training:
                optimizer.zero_grad(); loss.backward(); optimizer.step()
        total_loss += loss.item() * labels.size(0)
        total_correct += (logits.argmax(1) == labels).sum().item(); total += labels.size(0)
    return total_loss / total, total_correct / total

def fit(epochs, optimizer, best_accuracy=0.0):
    stale = 0
    for epoch in range(1, epochs + 1):
        train_loss, train_acc = run_epoch(train_loader, optimizer)
        val_loss, val_acc = run_epoch(val_loader)
        print(f'{epoch:02d}: train loss={train_loss:.4f} acc={train_acc:.3%} | val loss={val_loss:.4f} acc={val_acc:.3%}')
        if val_acc > best_accuracy:
            best_accuracy, stale = val_acc, 0
            torch.save({'model_state': model.state_dict(), 'image_size': IMAGE_SIZE, 'classes': ['empty', 'occupied'], 'val_accuracy': val_acc}, MODEL_PATH)
        else:
            stale += 1
            if stale >= PATIENCE: break
    return best_accuracy


# In[ ]:


for parameter in model.features.parameters(): parameter.requires_grad = False
optimizer = torch.optim.AdamW(filter(lambda p: p.requires_grad, model.parameters()), lr=1e-3, weight_decay=1e-4)
best_accuracy = fit(FROZEN_EPOCHS, optimizer)


# In[ ]:


for parameter in model.parameters(): parameter.requires_grad = True
optimizer = torch.optim.AdamW(model.parameters(), lr=1e-4, weight_decay=1e-4)
best_accuracy = fit(FINE_TUNE_EPOCHS, optimizer, best_accuracy)
checkpoint = torch.load(MODEL_PATH, map_location=DEVICE, weights_only=True)
model.load_state_dict(checkpoint['model_state'])
print(f"Best validation accuracy: {checkpoint['val_accuracy']:.3%}")


# ## Test and demo visualization

# In[ ]:


test_loss, test_accuracy = run_epoch(test_loader)
print(f'Test loss: {test_loss:.4f} | test accuracy: {test_accuracy:.3%}')


# In[ ]:


@torch.inference_mode()
def predict_full_image(image_path: Path):
    image_spots = [spot for spot in all_records if spot.image_path == image_path]
    if image_spots and image_spots[0].annotation_path.suffix == '.json':
        # Reload this image without sampling so the displayed percentage uses every space.
        annotation_path = image_spots[0].annotation_path
        with annotation_path.open(encoding='utf-8') as file:
            data = json.load(file)
        image_info = next(image for image in data['images'] if image['file_name'] == image_path.name)
        categories = {category['id']: category['name'].lower() for category in data['categories']}
        image_spots = []
        for annotation in data['annotations']:
            if annotation['image_id'] != image_info['id']: continue
            category = categories.get(annotation['category_id'], '')
            if category not in {'space-empty', 'space-occupied'}: continue
            x, y, width, height = annotation['bbox']
            image_spots.append(Spot(image_path, annotation_path, int(category == 'space-occupied'), image_path.name[:10], 'box', (x, y, x + width, y + height), 'test'))
    batch = torch.stack([eval_transform(crop_spot(spot)) for spot in image_spots]).to(DEVICE)
    probabilities = model(batch).softmax(1)[:, 1].cpu().numpy()
    predictions = probabilities >= 0.5
    canvas = cv2.imread(str(image_path))
    for spot, occupied, probability in zip(image_spots, predictions, probabilities):
        color = (0, 0, 255) if occupied else (0, 200, 0)
        if spot.kind == 'rotated':
            cx, cy, width, height, angle = spot.geometry
            points = cv2.boxPoints(((cx, cy), (width, height), angle)).astype(int)
            cv2.polylines(canvas, [points], True, color, 2)
        else:
            x1, y1, x2, y2 = map(round, spot.geometry); cv2.rectangle(canvas, (x1, y1), (x2, y2), color, 2)
    percentage = 100 * predictions.mean()
    canvas = cv2.cvtColor(canvas, cv2.COLOR_BGR2RGB)
    plt.figure(figsize=(16, 9)); plt.imshow(canvas); plt.axis('off'); plt.title(f'{predictions.sum()} / {len(predictions)} occupied — {percentage:.1f}% full', fontsize=18)
    return percentage

demo_image = random.choice(test_records).image_path
predict_full_image(demo_image)
plt.show()
