"""Routes for parking-lot occupancy predictions."""

from __future__ import annotations

from io import BytesIO

from fastapi import APIRouter, File, HTTPException, UploadFile, status
from PIL import Image, UnidentifiedImageError

from api.schemas.parking_lot import (
    BoundingBox,
    LotAnalysisResponse,
    PredictionResponse,
    RowResult,
    SpotResult,
)
from api.services.parking_lot import DEFAULT_MODEL_PATH, get_predictor
from api.services.parking_lot_structure import analyze_sample_layout


MAX_IMAGE_BYTES = 10 * 1024 * 1024
ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/webp"}

router = APIRouter(prefix="/parking-lot", tags=["parking lot"])


@router.get("/health")
def health() -> dict[str, str]:
    return {
        "status": "ok" if DEFAULT_MODEL_PATH.is_file() else "model_missing",
        "model": DEFAULT_MODEL_PATH.name,
    }


@router.post("/predict", response_model=PredictionResponse)
def predict(
    file: UploadFile = File(..., description="A cropped parking-space image"),
) -> PredictionResponse:
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


@router.post("/analyze", response_model=LotAnalysisResponse)
def analyze(
    file: UploadFile = File(..., description="A complete sample-layout image"),
) -> LotAnalysisResponse:
    image = _read_image(file)
    structure_id, analyzed_rows = analyze_sample_layout(image)
    rows = []
    free_spot_ids = []
    for row_number, analyzed_spots in enumerate(analyzed_rows, start=1):
        spots = []
        for analyzed in analyzed_spots:
            if analyzed.status == "empty":
                free_spot_ids.append(analyzed.id)
            x_min, y_min, x_max, y_max = analyzed.box
            spots.append(
                SpotResult(
                    id=analyzed.id,
                    row=analyzed.row,
                    number=analyzed.number,
                    status=analyzed.status,
                    confidence=analyzed.confidence,
                    bounding_box=BoundingBox(
                        x_min=x_min,
                        y_min=y_min,
                        x_max=x_max,
                        y_max=y_max,
                    ),
                )
            )
        free_count = sum(spot.status == "empty" for spot in spots)
        rows.append(
            RowResult(
                row=row_number,
                spot_count=len(spots),
                free_count=free_count,
                occupied_count=len(spots) - free_count,
                spots=spots,
            )
        )

    total_spots = sum(row.spot_count for row in rows)
    return LotAnalysisResponse(
        structure_id=structure_id,
        row_count=len(rows),
        total_spots=total_spots,
        free_count=len(free_spot_ids),
        occupied_count=total_spots - len(free_spot_ids),
        free_spot_ids=free_spot_ids,
        rows=rows,
    )


def _read_image(file: UploadFile) -> Image.Image:
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
        return image
    except (UnidentifiedImageError, OSError):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="The uploaded file is not a valid image.",
        ) from None
