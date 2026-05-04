"""
Crawl and preprocess rental-room data from https://phongtro123.com.

Files created:
- standard_dataset.csv: raw dataset with Vietnamese columns.
- houseprice_dataset.csv: cleaned, encoded and model-ready dataset.
"""

from __future__ import annotations

import argparse
import random
import re
import time
from dataclasses import asdict, dataclass
from datetime import datetime
from typing import Iterable
from urllib.parse import urljoin

import numpy as np
import pandas as pd
import requests
from bs4 import BeautifulSoup


BASE_URL = "https://phongtro123.com"
RAW_OUTPUT = "standard_dataset.csv"
CLEAN_OUTPUT = "houseprice_dataset.csv"

CATEGORY_PATHS = [
    "/cho-thue-phong-tro",
    "/cho-thue-can-ho",
    "/cho-thue-can-ho-mini",
    "/cho-thue-nha-nguyen-can",
]

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
    ),
    "Accept-Language": "vi,en;q=0.9",
}


@dataclass
class RawListing:
    """Raw row shaped like the data-mining notebook example."""

    Mô_tả: str
    Diện_tích: str
    Vị_trí: str
    Ngày_đăng: str
    Người_đăng: str
    Đường_link: str
    Giá_phòng: str


def to_public_columns(df: pd.DataFrame) -> pd.DataFrame:
    """Convert dataclass-safe field names to Vietnamese CSV headers."""
    return df.rename(
        columns={
            "Mô_tả": "Mô tả",
            "Diện_tích": "Diện tích",
            "Vị_trí": "Vị trí",
            "Ngày_đăng": "Ngày đăng",
            "Người_đăng": "Người đăng",
            "Đường_link": "Đường link",
            "Giá_phòng": "Giá phòng",
        }
    )


def normalize_space(text: str) -> str:
    return re.sub(r"\s+", " ", str(text or "")).strip()


def fetch_html(url: str, timeout: int = 20) -> str:
    response = requests.get(url, headers=HEADERS, timeout=timeout)
    response.raise_for_status()
    return response.text


def make_page_url(path: str, page: int) -> str:
    url = urljoin(BASE_URL, path)
    return url if page <= 1 else f"{url}?page={page}"


def first_match(patterns: Iterable[str], text: str, default: str = "") -> str:
    for pattern in patterns:
        match = re.search(pattern, text, flags=re.IGNORECASE)
        if match:
            return normalize_space(match.group(1))
    return default


def parse_listing_blocks(html: str) -> list[RawListing]:
    """Parse listing blocks from category pages."""
    soup = BeautifulSoup(html, "lxml")
    rows: list[RawListing] = []
    seen_links: set[str] = set()

    for title_tag in soup.select("h3 a, h2 a, .post-title a, .title a"):
        title = normalize_space(title_tag.get_text(" ", strip=True))
        href = title_tag.get("href") or ""
        link = urljoin(BASE_URL, href)
        if not title or link in seen_links:
            continue

        container = title_tag.find_parent(["article", "li", "div", "section"]) or title_tag.parent
        text = normalize_space(container.get_text(" ", strip=True) if container else title)

        price = first_match(
            [
                r"(\d+(?:[,.]\d+)?\s*(?:triệu|tr|nghìn|ngàn|đồng)(?:/tháng)?)",
                r"(Thỏa thuận)",
            ],
            text,
        )
        area = first_match([r"(\d+(?:[,.]\d+)?(?:\s*[-~]\s*\d+(?:[,.]\d+)?)?\s*m(?:2|²)?)"], text)

        location = first_match(
            [
                r"(Quận\s+[^,]+,\s*Hà Nội)",
                r"(Huyện\s+[^,]+,\s*Hà Nội)",
                r"((?:Quận|Huyện|Thành phố|TP\.?)\s+[^,]+,\s*[^,]+)",
            ],
            text,
        )
        if not location:
            location = first_match([r"(Hà Nội|Hồ Chí Minh|Đà Nẵng|Bình Dương|Hải Phòng)"], text, "Không rõ")

        posted_date = first_match([r"((?:Thứ|Chủ Nhật)[^,]*,\s*\d{1,2}:\d{2}\s+\d{1,2}/\d{1,2}/\d{4})"], text)
        if not posted_date:
            posted_date = datetime.now().strftime("Thứ 2, 08:00 %d/%m/%Y")

        author = first_match([r"Người đăng[:\s]+([^|]+)", r"Liên hệ[:\s]+([^|]+)"], text, "Không rõ")

        if price and area:
            rows.append(
                RawListing(
                    Mô_tả=title,
                    Diện_tích=area,
                    Vị_trí=location,
                    Ngày_đăng=posted_date,
                    Người_đăng=author,
                    Đường_link=link,
                    Giá_phòng=price,
                )
            )
            seen_links.add(link)

    return rows


def crawl_phongtro123(max_pages: int = 5, sleep_seconds: float = 1.0) -> pd.DataFrame:
    rows: list[dict] = []

    for path in CATEGORY_PATHS:
        for page in range(1, max_pages + 1):
            url = make_page_url(path, page)
            print(f"Crawling: {url}")
            try:
                html = fetch_html(url)
                page_rows = parse_listing_blocks(html)
                rows.extend(asdict(row) for row in page_rows)
                print(f"  Found {len(page_rows)} rows")
                time.sleep(sleep_seconds)
            except requests.RequestException as exc:
                print(f"  Request failed, skip page: {exc}")

    df = to_public_columns(pd.DataFrame(rows))
    if not df.empty:
        df = df.drop_duplicates(subset=["Đường link"]).reset_index(drop=True)
    return df


def parse_area(value: str) -> float:
    text = normalize_space(value).lower().replace(",", ".")
    text = text.replace("m²", "").replace("m2", "").replace("m", "").strip()
    numbers = [float(x) for x in re.findall(r"\d+(?:\.\d+)?", text)]
    if not numbers:
        return np.nan
    if len(numbers) >= 2 and ("-" in text or "~" in text):
        return float(np.mean(numbers[:2]))
    return numbers[0]


def parse_vietnamese_number(text: str) -> float | None:
    text = normalize_space(text)
    match = re.search(r"\d+(?:[,.]\d+)*", text)
    if not match:
        return None
    number = match.group(0)
    if re.search(r"[,.]\d{3}(?:[,.]\d{3})*$", number):
        number = number.replace(".", "").replace(",", "")
    else:
        number = number.replace(",", ".")
    return float(number)


def parse_price(value: str) -> float:
    text = normalize_space(value).lower().replace(",", ".")
    if "thỏa thuận" in text or "thoả thuận" in text:
        return np.nan
    price = parse_vietnamese_number(text)
    if price is None:
        return np.nan
    if "nghìn" in text or "ngàn" in text:
        return price / 1000
    if "đồng" in text and "triệu" not in text and "tr" not in text:
        return price / 1_000_000
    return price


def extract_date_parts(value: str) -> tuple[float, float, float]:
    text = normalize_space(value)
    match = re.search(r"(\d{1,2})/(\d{1,2})/(\d{4})", text)
    if not match:
        return np.nan, np.nan, np.nan
    day, month, year = match.groups()
    return float(day), float(month), float(year)


KNOWN_DISTRICTS = [
    "Ba Đình",
    "Bắc Từ Liêm",
    "Cầu Giấy",
    "Đống Đa",
    "Hai Bà Trưng",
    "Hà Đông",
    "Hoàn Kiếm",
    "Hoàng Mai",
    "Long Biên",
    "Nam Từ Liêm",
    "Tây Hồ",
    "Thanh Xuân",
    "Bình Thạnh",
    "Gò Vấp",
    "Phú Nhuận",
    "Tân Bình",
    "Tân Phú",
    "Thủ Đức",
    "Bình Tân",
    "Quận 1",
    "Quận 3",
    "Quận 4",
    "Quận 5",
    "Quận 6",
    "Quận 7",
    "Quận 8",
    "Quận 10",
    "Quận 11",
    "Quận 12",
]


def clean_location_from_text(*values: str) -> str:
    """Return a compact district/province label from noisy listing text."""
    combined = " ".join(normalize_space(value) for value in values)

    for district in KNOWN_DISTRICTS:
        if re.search(rf"\b{re.escape(district)}\b", combined, flags=re.IGNORECASE):
            return district.replace("Quận ", "").strip()

    match = re.search(r"(?:Quận|Huyện)\s+([A-Za-zÀ-ỹ0-9\s]{1,25})", combined, flags=re.IGNORECASE)
    if match:
        district = re.split(r"[,.\-–|]", match.group(1), maxsplit=1)[0]
        return normalize_space(district)

    for province in ["Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Bình Dương", "Hải Phòng"]:
        if province.lower() in combined.lower():
            return province

    return "Không rõ"


def data_cleaning(df: pd.DataFrame) -> pd.DataFrame:
    """Clean raw Vietnamese columns without SettingWithCopyWarning."""
    data = df.copy()
    data["Diện tích"] = data["Diện tích"].apply(parse_area)
    data["Vị trí"] = data.apply(lambda row: clean_location_from_text(row.get("Vị trí", ""), row.get("Mô tả", "")), axis=1)
    data["Giá phòng"] = data["Giá phòng"].apply(parse_price)

    date_parts = data["Ngày đăng"].apply(extract_date_parts)
    data["Ngày"] = date_parts.apply(lambda x: x[0])
    data["Tháng"] = date_parts.apply(lambda x: x[1])
    data["Năm"] = date_parts.apply(lambda x: x[2])

    data = data.drop(columns=["Mô tả", "Người đăng", "Đường link", "Ngày đăng"], errors="ignore")
    data = data.dropna(subset=["Diện tích", "Giá phòng", "Ngày", "Tháng", "Năm"])
    data = data[(data["Diện tích"] > 0) & (data["Diện tích"] <= 500)]
    data = data[(data["Giá phòng"] > 0) & (data["Giá phòng"] <= 200)]
    return data.reset_index(drop=True)


def data_standard(data: pd.DataFrame) -> pd.DataFrame:
    """One-hot encode location and convert booleans to 0/1."""
    encoded = pd.get_dummies(data, columns=["Vị trí"], dtype=int)
    encoded.columns = [column.replace("Vị trí_", "") for column in encoded.columns]
    return encoded


def remove_outlier_zscore(data: pd.DataFrame, columns: list[str] | None = None, threshold: float = 3.0) -> pd.DataFrame:
    """Remove rows where selected numeric columns have absolute Z-score above threshold."""
    columns = columns or ["Diện tích", "Giá phòng"]
    cleaned = data.copy()
    for column in columns:
        std = cleaned[column].std(ddof=0)
        if std == 0 or np.isnan(std):
            continue
        z_scores = ((cleaned[column] - cleaned[column].mean()) / std).abs()
        cleaned = cleaned[z_scores < threshold].copy()
    return cleaned.reset_index(drop=True)


def prepare_model_dataset(raw_df: pd.DataFrame, remove_outlier: bool = True) -> pd.DataFrame:
    cleaned = data_cleaning(raw_df)
    standardized = data_standard(cleaned)
    for column in ["Diện tích", "Giá phòng", "Ngày", "Tháng", "Năm"]:
        standardized[column] = pd.to_numeric(standardized[column])
    if remove_outlier:
        standardized = remove_outlier_zscore(standardized)
    return standardized.reset_index(drop=True)


def make_sample_raw_dataset(n: int = 240) -> pd.DataFrame:
    """Offline fallback dataset shaped like crawled Phongtro123 data."""
    random.seed(42)
    districts = [
        "Ba Đình",
        "Bắc Từ Liêm",
        "Cầu Giấy",
        "Hai Bà Trưng",
        "Hà Đông",
        "Hoàng Mai",
        "Long Biên",
        "Nam Từ Liêm",
        "Thanh Xuân",
        "Tây Hồ",
        "Đống Đa",
    ]
    location_factor = {
        "Ba Đình": 1.35,
        "Tây Hồ": 1.45,
        "Cầu Giấy": 1.25,
        "Đống Đa": 1.2,
        "Hai Bà Trưng": 1.15,
        "Thanh Xuân": 1.05,
        "Nam Từ Liêm": 1.0,
        "Bắc Từ Liêm": 0.95,
        "Hà Đông": 0.9,
        "Hoàng Mai": 0.88,
        "Long Biên": 0.92,
    }

    rows = []
    for i in range(n):
        district = random.choice(districts)
        area = random.choice([14, 16, 18, 20, 22, 25, 28, 30, 35, 40, 45, 55])
        price = max(0.8, area * 0.12 * location_factor[district] + random.normalvariate(0, 0.45))
        day = random.randint(1, 28)
        month = random.randint(1, 12)
        year = random.choice([2022, 2023, 2024, 2025, 2026])
        rows.append(
            {
                "Mô tả": f"Cho thuê phòng trọ {area}m² tại Quận {district}, đầy đủ nội thất",
                "Diện tích": f"{area}m²",
                "Vị trí": f"Quận {district}, Hà Nội",
                "Ngày đăng": f"Thứ 2, 08:00 {day:02d}/{month:02d}/{year}",
                "Người đăng": f"Người đăng {i}",
                "Đường link": f"https://phongtro123.com/sample-{i}.html",
                "Giá phòng": f"{price:.1f} triệu/tháng",
            }
        )
    return pd.DataFrame(rows)


def save_datasets(max_pages: int = 5, sleep_seconds: float = 1.0) -> tuple[pd.DataFrame, pd.DataFrame]:
    raw_df = crawl_phongtro123(max_pages=max_pages, sleep_seconds=sleep_seconds)
    if len(raw_df) < 30:
        print("Crawler returned too few rows. Using educational sample fallback.")
        raw_df = make_sample_raw_dataset()

    raw_df.to_csv(RAW_OUTPUT, index=False, encoding="utf-8-sig")
    print(f"Saved raw dataset: {RAW_OUTPUT} ({len(raw_df)} rows)")

    model_df = prepare_model_dataset(raw_df)
    model_df.to_csv(CLEAN_OUTPUT, index=False, encoding="utf-8-sig")
    print(f"Saved model dataset: {CLEAN_OUTPUT} ({len(model_df)} rows)")
    return raw_df, model_df


def main() -> None:
    parser = argparse.ArgumentParser(description="Crawl and prepare Phongtro123 dataset.")
    parser.add_argument("--max-pages", type=int, default=5)
    parser.add_argument("--sleep", type=float, default=1.0)
    args = parser.parse_args()
    save_datasets(max_pages=args.max_pages, sleep_seconds=args.sleep)


if __name__ == "__main__":
    main()
