"""Load the trained parking-space classifier and run predictions."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from threading import Lock

import torch
from PIL import Image
from torch import nn
from torchvision import transforms
from torchvision.models import mobilenet_v3_small


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MODEL_PATH = PROJECT_ROOT / "models" / "mobilenet_v3_small_pklot.pt"


@dataclass(frozen=True)
class Prediction:
    label: str
    confidence: float
    probabilities: dict[str, float]


class ParkingSpotPredictor:
    """MobileNetV3 classifier for a cropped image of one parking space."""

    def __init__(self, model_path: Path = DEFAULT_MODEL_PATH) -> None:
        if not model_path.is_file():
            raise FileNotFoundError(f"Model checkpoint not found: {model_path}")

        checkpoint = torch.load(model_path, map_location="cpu", weights_only=True)
        self.classes = tuple(checkpoint.get("classes", ("empty", "occupied")))
        if len(self.classes) != 2:
            raise ValueError("The model checkpoint must contain exactly two classes")

        image_size = int(checkpoint.get("image_size", 160))
        model = mobilenet_v3_small(weights=None)
        model.classifier[-1] = nn.Linear(
            model.classifier[-1].in_features, len(self.classes)
        )
        model.load_state_dict(checkpoint["model_state"])
        model.eval()
        self.model = model
        self.transform = transforms.Compose(
            [
                transforms.Resize((image_size, image_size)),
                transforms.ToTensor(),
                transforms.Normalize(
                    [0.485, 0.456, 0.406],
                    [0.229, 0.224, 0.225],
                ),
            ]
        )

    @torch.inference_mode()
    def predict(self, image: Image.Image) -> Prediction:
        return self.predict_many([image])[0]

    @torch.inference_mode()
    def predict_many(self, images: list[Image.Image]) -> list[Prediction]:
        """Classify several parking-space crops in one model call."""
        if not images:
            return []
        tensors = torch.stack(
            [self.transform(image.convert("RGB")) for image in images]
        )
        batch_probabilities = self.model(tensors).softmax(dim=1).tolist()
        predictions = []
        for probabilities in batch_probabilities:
            class_probabilities = dict(
                zip(self.classes, probabilities, strict=True)
            )
            label = max(
                class_probabilities, key=class_probabilities.get
            )  # type: ignore[arg-type]
            predictions.append(
                Prediction(
                    label=label,
                    confidence=class_probabilities[label],
                    probabilities=class_probabilities,
                )
            )
        return predictions


_predictor: ParkingSpotPredictor | None = None
_predictor_lock = Lock()


def get_predictor() -> ParkingSpotPredictor:
    """Return a lazily initialized process-wide predictor."""
    global _predictor
    if _predictor is None:
        with _predictor_lock:
            if _predictor is None:
                _predictor = ParkingSpotPredictor()
    return _predictor
