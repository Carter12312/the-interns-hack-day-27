import httpx
from pathlib import Path


JS_URL = "https://order.eatify.io/main.412d31db13d8d231.js"


def fetch_js():
    print("Downloading JS bundle...")

    response = httpx.get(JS_URL)
    response.raise_for_status()

    print(f"Downloaded {len(response.text):,} characters")

    return response.text


def find_context(js, keyword, output, amount=500, max_results=10):
    output.write("\n")
    output.write("=" * 100 + "\n")
    output.write(f"SEARCH: {keyword}\n")
    output.write("=" * 100 + "\n\n")

    start = 0
    count = 0

    while True:
        index = js.find(keyword, start)

        if index == -1 or count >= max_results:
            break

        before = max(0, index - amount)
        after = min(len(js), index + len(keyword) + amount)

        output.write(js[before:after])
        output.write("\n\n--- MATCH END ---\n\n")

        start = index + len(keyword)
        count += 1

    output.write(f"\nFound {count} matches shown.\n")


def main():
    js = fetch_js()

    searches = [
    "menus(",
    ".menus(",
    "menus:",
    "menus=",
    "locationService",
    ]

    with open(
        "eatify_js_search_results.txt",
        "w",
        encoding="utf-8"
    ) as output:

        output.write(
            f"Eatify JS Investigation\n"
            f"Bundle: {JS_URL}\n"
            f"Size: {len(js):,} characters\n"
        )
        find_context(js, "class Je", output, 5000, 50)

    print("Finished!")
    print("Results saved to eatify_js_search_results.txt")


if __name__ == "__main__":
    main()