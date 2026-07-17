from fastapi import FastAPI, HTTPException
import json
import random
from pathlib import Path

app = FastAPI()

MENU_FILE = Path("menu.json")


def load_restaurants():
    with open(MENU_FILE, "r", encoding="utf-8") as file:
        data = json.load(file)
        return data["restaurants"]


@app.get("/restaurants")
def get_restaurants():
    """Return all available restaurants."""
    restaurants = load_restaurants()

    return [
        {
            "id": restaurant["id"],
            "name": restaurant["name"]
        }
        for restaurant in restaurants
    ]


@app.get("/menu/{restaurant_id}")
def get_menu(restaurant_id: str):
    """Get a restaurant menu and estimated wait time."""

    restaurants = load_restaurants()

    restaurant = next(
        (
            r for r in restaurants
            if r["id"] == restaurant_id
        ),
        None
    )

    if restaurant is None:
        raise HTTPException(
            status_code=404,
            detail="Restaurant not found"
        )

    return {
        "restaurant": restaurant["name"],
        "menu": restaurant["menu"],
        "estimated_wait_time": random.randint(5, 30)
    }