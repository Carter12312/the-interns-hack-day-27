"""Generate API-ready inference snapshots for Corporate HQ lots A-F."""

from __future__ import annotations

from pathlib import Path
import sys

from PIL import Image

PROJECT_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT_ROOT))

from api.routers.parking_lot import _analyze_image  # noqa: E402


SAMPLE_DIRECTORY = PROJECT_ROOT / "parking_lot_samples"
OUTPUT_DIRECTORY = PROJECT_ROOT / "api" / "data" / "precomputed"


def main() -> None:
    OUTPUT_DIRECTORY.mkdir(parents=True, exist_ok=True)
    for lot_id in "ABCDEF":
        image_path = SAMPLE_DIRECTORY / f"corporateHeadquarters{lot_id}.jpg"
        if not image_path.is_file():
            raise FileNotFoundError(f"Missing Corporate HQ image: {image_path}")
        with Image.open(image_path) as source:
            source.load()
            analysis = _analyze_image(source.copy())
        output_path = OUTPUT_DIRECTORY / f"corporateHeadquarters{lot_id}.json"
        output_path.write_text(
            analysis.model_dump_json(indent=2),
            encoding="utf-8",
        )
        print(
            f"Lot {lot_id}: {analysis.free_count}/{analysis.total_spots} free -> "
            f"{output_path.relative_to(PROJECT_ROOT)}"
        )


if __name__ == "__main__":
    main()
