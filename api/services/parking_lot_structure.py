"""Apply the sample parking-lot structure and classify each known space."""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

from PIL import Image

from api.services.parking_lot import get_predictor


STRUCTURE_PATH = Path(__file__).resolve().parents[1] / "data" / "parking_lot_structure.json"


@dataclass(frozen=True)
class StructuredSpot:
    id: str
    row: int
    number: int
    status: str
    confidence: float
    box: tuple[float, float, float, float]


def analyze_sample_layout(image: Image.Image) -> tuple[str, list[list[StructuredSpot]]]:
    """Classify every location in the normalized sample-image layout."""
    structure = json.loads(STRUCTURE_PATH.read_text(encoding="utf-8"))
    width, height = image.size
    definitions = []
    crops = []
    for row in structure["rows"]:
        for spot in row["spots"]:
            normalized = spot["box"]
            box = (
                normalized["x_min"] * width,
                normalized["y_min"] * height,
                normalized["x_max"] * width,
                normalized["y_max"] * height,
            )
            definitions.append((spot["id"], row["row"], spot["number"], box))
            crops.append(image.crop(box))

    predictions = get_predictor().predict_many(crops)
    rows: list[list[StructuredSpot]] = [[] for _ in structure["rows"]]
    for definition, prediction in zip(definitions, predictions, strict=True):
        spot_id, row_number, spot_number, box = definition
        rows[row_number - 1].append(
            StructuredSpot(
                id=spot_id,
                row=row_number,
                number=spot_number,
                status=prediction.label,
                confidence=prediction.confidence,
                box=box,
            )
        )
    return structure["structure_id"], rows
