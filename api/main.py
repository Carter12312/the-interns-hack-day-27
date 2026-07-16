"""Application entry point for all backend APIs."""

from fastapi import FastAPI

from api.routers import parking_lot


app = FastAPI(
    title="Prediction API",
    description="Prediction endpoints for the project's trained models.",
    version="1.0.0",
)

app.include_router(parking_lot.router, prefix="/api/v1")


@app.get("/health", tags=["system"])
def health() -> dict[str, str]:
    return {"status": "ok"}
