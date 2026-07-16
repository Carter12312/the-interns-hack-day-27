"""Routes for parking-lot occupancy predictions."""

from __future__ import annotations

from io import BytesIO

from pathlib import Path

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
SAMPLE_DIRECTORY = Path(__file__).resolve().parents[2] / "parking_lot_samples"
PRECOMPUTED_DIRECTORY = Path(__file__).resolve().parents[1] / "data" / "precomputed"


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
    return _analyze_image(image)


@router.get("/samples/{sample_id}/analysis", response_model=LotAnalysisResponse)
def analyze_sample(sample_id: str) -> LotAnalysisResponse:
    """Analyze one of the POC sample lots (A-H) without a multipart upload."""
    normalized_id = sample_id.strip().upper()
    if normalized_id not in set("ABCDEFGH"):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Sample lot must be one of A through H.",
        )
    image_path = SAMPLE_DIRECTORY / f"parkingLot{normalized_id}.jpg"
    if not image_path.is_file():
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Sample lot {normalized_id} is unavailable.",
        )
    with Image.open(image_path) as source:
        source.load()
        image = source.copy()
    return _analyze_image(image)


@router.get(
    "/locations/corporate-hq/lots/{lot_id}/analysis",
    response_model=LotAnalysisResponse,
)
def analyze_corporate_hq_lot(lot_id: str) -> LotAnalysisResponse:
    """Analyze Bloomington Corporate HQ lot A-F when its image is available."""
    normalized_id = lot_id.strip().upper()
    if normalized_id not in set("ABCDEF"):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Corporate HQ lot must be one of A through F.",
        )
    image_path = SAMPLE_DIRECTORY / f"corporateHeadquarters{normalized_id}.jpg"
    if not image_path.is_file():
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=(
                f"Corporate HQ lot {normalized_id} is not configured yet. "
                f"Add parking_lot_samples/{image_path.name}."
            ),
        )
    analysis_path = PRECOMPUTED_DIRECTORY / f"corporateHeadquarters{normalized_id}.json"
    if not analysis_path.is_file():
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=(
                f"Precomputed analysis for Corporate HQ lot {normalized_id} is missing. "
                "Run parking-lot-bot/precompute_corporate_hq.py."
            ),
        )
    return LotAnalysisResponse.model_validate_json(
        analysis_path.read_text(encoding="utf-8")
    )


@router.get("/locations/corporate-hq/summary")
def corporate_hq_summary() -> dict[str, list[dict[str, int | str]]]:
    """Return fast availability totals from the precomputed A-F snapshots."""
    lots: list[dict[str, int | str]] = []
    for lot_id in "ABCDEF":
        analysis_path = PRECOMPUTED_DIRECTORY / f"corporateHeadquarters{lot_id}.json"
        if not analysis_path.is_file():
            continue
        analysis = LotAnalysisResponse.model_validate_json(
            analysis_path.read_text(encoding="utf-8")
        )
        lots.append(
            {
                "lot_id": lot_id,
                "free_count": analysis.free_count,
                "occupied_count": analysis.occupied_count,
                "total_spots": analysis.total_spots,
            }
        )
    return {"lots": lots}


def _analyze_image(image: Image.Image) -> LotAnalysisResponse:
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
        image_width=image.width,
        image_height=image.height,
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
