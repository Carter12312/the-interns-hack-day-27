from bs4 import BeautifulSoup
import httpx

html = httpx.get(
    "https://order.eatify.io/2140/7927/flow/pickup"
).text

soup = BeautifulSoup(html, "html.parser")

config = soup.select_one(".wo-hidden-configs")

if config:
    print(config.text[:2000])