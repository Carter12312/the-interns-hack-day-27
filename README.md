# Hub Companion and Parking Intelligence POC

This repository contains two independent applications. Open and run each from
its own project root:

```text
the-interns-hack-day-27/
├── mobile-app/          Kotlin Multiplatform app (Android, iOS, desktop demo)
├── api/                 FastAPI inference and lot-analysis service
├── parking-lot-bot/     Model training scripts and notebooks
├── models/              Trained MobileNet checkpoint
└── requirements.txt     Python dependencies
```

## Run the mobile app

You can now open either the repository root or the `mobile-app/` directory in
Android Studio. The repository root contains a small composite-build bridge
that imports the self-contained mobile project.

From a terminal:

```bash
cd mobile-app
./gradlew :composeApp:assembleDebug
```

From the repository root, the equivalent command is:

```bash
./gradlew :mobile-app:composeApp:assembleDebug
```

The Android application module is `composeApp`. The repository-level Python
environment and `.idea` run configuration are unrelated to the Android build.

## Parking model

Download the PKLot dataset and create a CSV containing the occupied, empty, and
total spot counts for every annotated parking-lot image.

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python parking-lot-bot/load_data.py
```

The Kaggle client requires an API token. Create one in your Kaggle account
settings and save it as `~/.kaggle/kaggle.json`, or set the `KAGGLE_USERNAME`
and `KAGGLE_KEY` environment variables.

The script downloads data into `data/pklot/` and writes
`data/pklot_manifest.csv`. To index an existing download instead, run:

```bash
python parking-lot-bot/load_data.py --skip-download --data-dir /path/to/pklot
```

Both the loader and training pipeline check for existing images plus supported
COCO JSON or XML annotations before contacting Kaggle. Re-running them will use
the existing `data/pklot/` download.

The occupancy target is calculated as:

```text
occupancy_percentage = occupied_spots / total_spots * 100
```

## Train the model

Install the dependencies and register the project environment as a Jupyter
kernel:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
python -m ipykernel install --user --name parking-lot-bot --display-name "Parking Lot Bot"
jupyter lab
```

Open `parking-lot-bot/train_mobilenet.ipynb`, choose the **Parking Lot Bot**
kernel, and run all cells. The notebook downloads PKLot if necessary, prepares
rotated parking-space crops, fine-tunes MobileNetV3-Small, saves the best model
to `models/mobilenet_v3_small_pklot.pt`, and visualizes predictions on a full
parking-lot image.

By default, training uses a balanced demo subset: 2,500 empty and 2,500
occupied training crops, plus 500 of each class for validation and testing.
Change `MAX_SAMPLES_PER_CLASS` and `MAX_EVAL_SAMPLES_PER_CLASS` in the
configuration cell if you want a different tradeoff. Set them to `None` to use
every annotated parking space.

### Run from PyCharm

The project includes a **Train MobileNet** run configuration that uses
`.venv/bin/python`. After reopening the project (or reloading run
configurations), choose **Train MobileNet** in the toolbar and click Run.

The same IDE-friendly training script can be started from PyCharm's terminal:

```bash
source .venv/bin/activate
python parking-lot-bot/train_mobilenet.py
```

The notebook remains useful for inspecting intermediate crops and charts; the
Python script runs the same training pipeline from beginning to end.

## Run the prediction API

The service uses **FastAPI**. It can classify one cropped parking-space image or
analyze the complete normalized sample-lot layout and return per-space status.

The API accepts a cropped image of one parking space and predicts whether it is
empty or occupied. Start it from the project root after installing the
requirements:

```bash
uvicorn api.main:app --reload
```

Send a JPEG, PNG, or WebP image to the prediction endpoint:

```bash
curl -X POST http://127.0.0.1:8000/api/v1/parking-lot/predict \
  -F "file=@parking-space.jpg"
```

Example response:

```json
{
  "label": "occupied",
  "confidence": 0.97,
  "probabilities": {"empty": 0.03, "occupied": 0.97}
}
```

Interactive API documentation is available at `http://127.0.0.1:8000/docs`.
`GET /health` reports the overall API status, while
`GET /api/v1/parking-lot/health` reports whether the parking-lot model is
available.

### Analyze a complete sample parking lot

The eight images in `parking_lot_samples/` share one annotated structure with
five rows containing 22, 22, 22, 22, and 12 spaces. The API applies this known
structure to the uploaded image and classifies every space:

```bash
curl -X POST http://127.0.0.1:8000/api/v1/parking-lot/analyze \
  -F "file=@parking_lot_samples/parkingLotA.jpg"
```

This route is intended for the sample camera layout. Its coordinates are
normalized, so resized versions work, but an image from a different camera or
lot requires another structure definition.

#### Parking-space identifier syntax

Space IDs use `Rrr-Sss`, where:

- `Rrr` is the one-based, zero-padded row number.
- `Sss` is the one-based, zero-padded space number within that row.
- Rows are assigned in spatial reading order from top to bottom.
- Spaces within each row are assigned from left to right.

For example, `R03-S08` means the eighth space in the third row. The API returns
`free_spot_ids` for quick lookup and a complete `rows` collection. Each spot in
that collection includes `id`, `row`, `number`, `status`, `confidence`, and a
pixel `bounding_box` (`x_min`, `y_min`, `x_max`, `y_max`) suitable for drawing
on the uploaded image.
