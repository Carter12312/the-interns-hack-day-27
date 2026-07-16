import httpx

BASE_URL = "https://order.eatify.io"

JS_URL = f"{BASE_URL}/main.412d31db13d8d231.js"


def fetch_js():
    print("Downloading Angular bundle...")

    response = httpx.get(JS_URL)

    response.raise_for_status()

    print(f"Downloaded {len(response.text):,} characters")

    return response.text


def search_js(js):
    keywords = [
        "api",
        "menu",
        "Menu",
        "restaurant",
        "location",
        "category",
        "item",
        "product",
        "order"
    ]

    for keyword in keywords:
        print(f"{keyword}: {js.count(keyword)}")

def find_context(js, keyword, amount=500):
    print("\n" + "=" * 80)
    print(f"SEARCHING: {keyword}")
    print("=" * 80)

    start = 0
    count = 0

    while True:
        index = js.find(keyword, start)

        if index == -1:
            break

        print(js[max(0, index-amount):index+amount])
        print("\n--- NEXT MATCH ---\n")

        start = index + len(keyword)
        count += 1

        if count >= 5:
            break


if __name__ == "__main__":
    js = fetch_js()

    find_context(js, "http.get", 200)
    find_context(js, "http.post", 200)
    find_context(js, ".get(", 200)
    find_context(js, ".post(", 200)
    find_context(js, "/api/", 200)
    find_context(js, "api/", 200)