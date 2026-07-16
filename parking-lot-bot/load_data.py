#!/usr/bin/env python3
"""Download PKLot from Kaggle and build a per-image occupancy manifest."""

from __future__ import annotations

import argparse
import csv
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


DATASET = "ammarnassanalhajali/pklot-dataset"
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".bmp"}


def dataset_is_installed(destination: Path) -> bool:
    """Return true when images and a supported annotation format are present."""
    if not destination.exists():
        return False
    has_images = any(
        path.suffix.lower() in IMAGE_SUFFIXES for path in destination.rglob("*")
    )
    has_annotations = any(destination.rglob("*.xml")) or any(
        destination.rglob("*_annotations.coco.json")
    )
    return has_images and has_annotations


@dataclass(frozen=True)
class ParkingImage:
    image_path: Path
    annotation_path: Path
    occupied_spots: int
    empty_spots: int

    @property
    def total_spots(self) -> int:
        return self.occupied_spots + self.empty_spots

    @property
    def occupancy_percentage(self) -> float:
        return 100.0 * self.occupied_spots / self.total_spots


def download_dataset(destination: Path) -> None:
    """Download and unzip PKLot using the Kaggle command-line client."""
    if dataset_is_installed(destination):
        print(f"Dataset already installed: {destination.resolve()}")
        return
    destination.mkdir(parents=True, exist_ok=True)
    kaggle_executable = Path(sys.executable).with_name("kaggle")
    if not kaggle_executable.exists():
        raise RuntimeError(
            f"Kaggle CLI not found at {kaggle_executable}. "
            "Install the project dependencies with: pip install -r requirements.txt"
        )
    command = [
        str(kaggle_executable),
        "datasets",
        "download",
        "-d",
        DATASET,
        "-p",
        str(destination),
        "--unzip",
    ]
    subprocess.run(command, check=True)


def normalize_label(label: str) -> str | None:
    """Map common PKLot annotation names to occupied/empty."""
    normalized = label.strip().lower().replace("-", "_").replace(" ", "_")
    if normalized in {"occupied", "not_free", "not_free_parking_space", "1"}:
        return "occupied"
    if normalized in {"empty", "free", "free_parking_space", "vacant", "0"}:
        return "empty"
    return None


def parse_voc_annotation(annotation_path: Path, image_path: Path) -> ParkingImage:
    root = ET.parse(annotation_path).getroot()
    labels = [normalize_label(node.text or "") for node in root.findall(".//object/name")]
    labels = [label for label in labels if label is not None]
    # Original PKLot XML stores occupancy as an attribute on each space.
    if not labels:
        for space in root.findall(".//space"):
            occupied = space.get("occupied")
            if occupied == "1":
                labels.append("occupied")
            elif occupied == "0":
                labels.append("empty")
    if not labels:
        raise ValueError(f"No occupied/empty labels found in {annotation_path}")
    return ParkingImage(
        image_path=image_path,
        annotation_path=annotation_path,
        occupied_spots=labels.count("occupied"),
        empty_spots=labels.count("empty"),
    )


def find_voc_samples(dataset_root: Path) -> list[ParkingImage]:
    """Pair images with same-named Pascal VOC XML files anywhere in the archive."""
    xml_by_stem: dict[str, list[Path]] = {}
    for xml_path in dataset_root.rglob("*.xml"):
        xml_by_stem.setdefault(xml_path.stem, []).append(xml_path)

    samples: list[ParkingImage] = []
    for image_path in dataset_root.rglob("*"):
        if image_path.suffix.lower() not in IMAGE_SUFFIXES:
            continue
        candidates = xml_by_stem.get(image_path.stem, [])
        if len(candidates) != 1:
            continue
        try:
            samples.append(parse_voc_annotation(candidates[0], image_path))
        except (ET.ParseError, ValueError):
            continue
    return sorted(samples, key=lambda sample: str(sample.image_path))


def write_manifest(samples: list[ParkingImage], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", newline="", encoding="utf-8") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=[
                "image_path",
                "annotation_path",
                "occupied_spots",
                "empty_spots",
                "total_spots",
                "occupancy_percentage",
            ],
        )
        writer.writeheader()
        for sample in samples:
            writer.writerow(
                {
                    "image_path": sample.image_path,
                    "annotation_path": sample.annotation_path,
                    "occupied_spots": sample.occupied_spots,
                    "empty_spots": sample.empty_spots,
                    "total_spots": sample.total_spots,
                    "occupancy_percentage": f"{sample.occupancy_percentage:.2f}",
                }
            )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data-dir", type=Path, default=Path("data/pklot"))
    parser.add_argument("--manifest", type=Path, default=Path("data/pklot_manifest.csv"))
    parser.add_argument(
        "--skip-download",
        action="store_true",
        help="Index an already-downloaded PKLot directory.",
    )
    args = parser.parse_args()

    if not args.skip_download:
        download_dataset(args.data_dir)
    if not args.data_dir.exists():
        parser.error(f"Data directory does not exist: {args.data_dir}")

    samples = find_voc_samples(args.data_dir)
    if not samples:
        parser.error(
            "No image/XML annotation pairs were found. Check --data-dir and ensure "
            "a PKLot XML-annotated version is present."
        )

    write_manifest(samples, args.manifest)
    print(f"Loaded {len(samples):,} annotated images")
    print(f"Manifest: {args.manifest.resolve()}")
    print(f"First image occupancy: {samples[0].occupancy_percentage:.2f}%")


if __name__ == "__main__":
    main()
