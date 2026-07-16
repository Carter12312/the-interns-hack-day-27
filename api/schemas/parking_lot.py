"""Schemas returned by the parking-lot API."""

from pydantic import BaseModel


class PredictionResponse(BaseModel):
    label: str
    confidence: float
    probabilities: dict[str, float]


class BoundingBox(BaseModel):
    x_min: float
    y_min: float
    x_max: float
    y_max: float


class SpotResult(BaseModel):
    id: str
    row: int
    number: int
    status: str
    confidence: float
    bounding_box: BoundingBox


class RowResult(BaseModel):
    row: int
    spot_count: int
    free_count: int
    occupied_count: int
    spots: list[SpotResult]


class LotAnalysisResponse(BaseModel):
    structure_id: str
    row_count: int
    total_spots: int
    free_count: int
    occupied_count: int
    free_spot_ids: list[str]
    rows: list[RowResult]
