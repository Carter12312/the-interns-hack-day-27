# Hub Companion and Parking Intelligence POC

For a workstation using VS Code without Android Studio, follow
[`VSCODE_DEVELOPMENT.md`](VSCODE_DEVELOPMENT.md). The repository includes VS
Code tasks and scripts for setup, FastAPI, the emulator, app installation, and
logs.

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

## Parking Lot API

The API classifies individual parking spaces and analyzes complete images that
use the included sample parking-lot layout.

## Setup and run

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
python -m uvicorn api.main:app --reload
```

The API runs at `http://127.0.0.1:8000`. Interactive OpenAPI documentation is
available at `http://127.0.0.1:8000/docs`.

Uploads must be JPEG, PNG, or WebP images no larger than 10 MB.

## Endpoints

### `GET /health`

Returns the overall API status.

```json
{"status": "ok"}
```

### `GET /api/v1/parking-lot/health`

Reports whether the parking-space classification model is available.

```json
{
  "status": "ok",
  "model": "mobilenet_v3_small_pklot.pt"
}
```

### `POST /api/v1/parking-lot/predict`

Classifies a cropped image of one parking space. Send the image as multipart
form data using the field name `file`.

```bash
curl -X POST http://127.0.0.1:8000/api/v1/parking-lot/predict \
  -F "file=@parking-space.jpg"
```

Response:

```json
{
  "label": "occupied",
  "confidence": 0.97,
  "probabilities": {
    "empty": 0.03,
    "occupied": 0.97
  }
}
```

`confidence` and the values in `probabilities` range from `0` to `1`.

The Android emulator reaches this local API at `http://10.0.2.2:8000`. Start
Uvicorn on the development Mac before opening a parking lot in the app:

```bash
uvicorn api.main:app --host 0.0.0.0 --port 8000 --reload
```

For the POC, the app maps its three lot cards to sample lots A, B, and C. The
read-only route below runs the model against a repository sample and returns
the same per-space analysis used by the rendered lot view:

```bash
curl http://127.0.0.1:8000/api/v1/parking-lot/samples/A/analysis
```

### `POST /api/v1/parking-lot/analyze`

Classifies every known space in a complete image using the layout shared by the
six `corporateHeadquarters` images in `parking_lot_samples/`. Send the image
using the field name `file`.

```bash
curl -X POST http://127.0.0.1:8000/api/v1/parking-lot/analyze \
  -F "file=@parking_lot_samples/corporateHeadquartersA.jpg"
```

Response structure:

```json
{
  "structure_id": "pklot-sample-layout-v1",
  "row_count": 5,
  "total_spots": 100,
  "free_count": 16,
  "occupied_count": 84,
  "free_spot_ids": ["R01-S06", "R02-S09"],
  "rows": [
    {
      "row": 1,
      "spot_count": 22,
      "free_count": 3,
      "occupied_count": 19,
      "spots": [
        {
          "id": "R01-S01",
          "row": 1,
          "number": 1,
          "status": "occupied",
          "confidence": 0.92,
          "bounding_box": {
            "x_min": 139.0,
            "y_min": 165.0,
            "x_max": 162.0,
            "y_max": 205.0
          }
        }
      ]
    }
  ]
}
```

The `bounding_box` values are pixel coordinates for drawing the result over the
uploaded image.

### Precompute Corporate HQ lots

Corporate HQ lots A-F are served from checked-in inference snapshots so mobile
requests do not run or load the model. Regenerate the snapshots after changing
an image, structure, or model:

```bash
.venv/bin/python parking-lot-bot/precompute_corporate_hq.py
```

## Parking-space identifier syntax

Space IDs use `Rrr-Sss`:

- `Rrr` is the one-based, zero-padded row number.
- `Sss` is the one-based, zero-padded space number within that row.
- Rows are numbered from top to bottom.
- Spaces within a row are numbered from left to right.

For example, `R03-S08` identifies the eighth space in the third row.

## Layout limitation

The complete-lot endpoint uses the five-row structure from the included sample
images: 22, 22, 22, 22, and 12 spaces. Coordinates are normalized, so resized
versions of that view work. Images from another camera angle or parking lot
require a separate structure definition.
