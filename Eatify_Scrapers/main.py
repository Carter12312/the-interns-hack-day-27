from fastapi import FastAPI
import json
import random
from pathlib import Path

app = FastAPI()

MENU_FILE = Path("menu.json")

def load_menu():
    with open(MENU_FILE, "r", encoding="utf-8") as file:
        return json.load(file)

@app.get("/menu")
def get_menu():
    """Get the menu for the restaurant and estimated wait time for the order."""
    return {
        "menu":load_menu(),
        "estimated_wait_time": random.randint(5, 30)
        }