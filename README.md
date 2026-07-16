# Parking Lot Occupancy

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
