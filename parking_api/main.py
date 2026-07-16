"""HTTP API for parking-space occupancy predictions."""

from __future__ import annotations

from io import BytesIO

from fastapi import FastAPI, File, HTTPException, UploadFile, status
from PIL import Image, UnidentifiedImageError
from pydantic import BaseModel

from parking_api.predictor import DEFAULT_MODEL_PATH, get_predictor


MAX_IMAGE_BYTES = 10 * 1024 * 1024
ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp"}

app = FastAPI(
    title="Parking Lot Prediction API",
    description="Classify a cropped parking space as empty or occupied.",
    version="1.0.0",
)


class PredictionResponse(BaseModel):
    label: str
    confidence: float
    probabilities: dict[str, float]


@app.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "ok" if DEFAULT_MODEL_PATH.is_file() else "model_missing",
        "model": DEFAULT_MODEL_PATH.name,
    }


@app.post("/predict", response_model=PredictionResponse)
def predict(file: UploadFile = File(..., description="A cropped parking-space image")) -> PredictionResponse:
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail="Upload a JPEG, PNG, or WebP image.",
        )

    contents = file.file.read(MAX_IMAGE_BYTES + 1)
    if len(contents) > MAX_IMAGE_BYTES:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail="Image must be 10 MB or smaller.",
        )

    try:
        image = Image.open(BytesIO(contents))
        image.load()
    except (UnidentifiedImageError, OSError):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="The uploaded file is not a valid image.",
        ) from None

    prediction = get_predictor().predict(image)
    return PredictionResponse(
        label=prediction.label,
        confidence=prediction.confidence,
        probabilities=prediction.probabilities,
    )
