"""Round 2 of the probe: Commons 4K candidates per category with contact sheets, so the picks are made by eye.
Writes zaytoon/lab/out/candidates.json and sheet_<category>.jpg (numbered thumbnails)."""
import io, json, os, re, time, urllib.parse, urllib.request
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "out")
os.makedirs(OUT, exist_ok=True)
UA = {"User-Agent": "ZaytoonWallpaperProbe/1.0 (https://github.com/ielkun10/cleanyourtv; personal use)"}
OK_LIC = ("cc0", "cc by", "public domain")
SEARCHES = {
    "makkah": ["Masjid al-Haram Kaaba", "Masjid al-Haram night", "Abraj Al Bait", "Mecca mountains Saudi"],
    "madinah": ["Al-Masjid an-Nabawi", "Green Dome Prophet's Mosque", "Masjid Nabawi umbrellas", "Quba Mosque"],
    "alula": ["Hegra Mada'in Salih", "Elephant Rock AlUla", "AlUla", "Maraya AlUla"],
    "asir": ["Abha Saudi Arabia", "Asir Province", "Al Soudah", "Rijal Almaa", "Habala", "Asir mountains"],
    "hijaz": ["Taif Saudi Arabia", "Al Hada Taif", "Al Shafa Taif", "Hejaz mountains"],
    "najd": ["Edge of the World Riyadh", "Tuwaiq", "Riyadh skyline", "Diriyah"],
    "desert": ["Empty Quarter Saudi Arabia", "Rub al Khali", "Nafud desert", "sand dunes Saudi Arabia"],
    "redsea": ["Red Sea Saudi Arabia", "Umluj", "Farasan Islands", "Jeddah corniche"],
}
SKIP = re.compile(r"bingo|winschoten|chester|stevenage|london|minnesota|ocean city|temple mount|aurangabad|"
                  r"map |khalili|chromolithograph|painting|manuscript|dala'il|1900 view|1725 view|"
                  r"parkir|interior|foyer|racks|city hall|receipt|candy|demolition|talismanic|calligraphy", re.I)

def get(url, raw=False):
    for attempt in range(6):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=60) as r:
                data = r.read()
            return data if raw else json.loads(data)
        except urllib.error.HTTPError as e:
            if e.code == 429:
                time.sleep(15 * (attempt + 1)); continue
            raise
    raise RuntimeError("429 " + url)

def search(term):
    q = {"action": "query", "format": "json", "generator": "search", "gsrnamespace": "6",
         "gsrsearch": f"{term} filew:>3839 filetype:bitmap", "gsrlimit": "30",
         "prop": "imageinfo", "iiprop": "url|size|extmetadata|mime", "iiurlwidth": "3840"}
    data = get("https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode(q))
    rows = []
    for p in (data.get("query", {}).get("pages", {}) or {}).values():
        ii = (p.get("imageinfo") or [{}])[0]
        md = ii.get("extmetadata", {})
        lic = md.get("LicenseShortName", {}).get("value") or ""
        w, h = ii.get("width", 0), ii.get("height", 0)
        if not any(k in lic.lower() for k in OK_LIC) or w < 3840 or h == 0 or w / h < 1.3 or SKIP.search(p["title"]):
            continue
        if ii.get("mime") not in ("image/jpeg", "image/png"):
            continue
        rows.append({"title": p["title"], "w": w, "h": h, "license": lic,
                     "license_url": md.get("LicenseUrl", {}).get("value"),
                     "artist": re.sub(r"<[^>]+>", "", md.get("Artist", {}).get("value") or "").strip(),
                     "page": ii.get("descriptionurl"), "thumb": ii.get("thumburl"),
                     "thumb_w": ii.get("thumbwidth"), "thumb_h": ii.get("thumbheight"), "term": term})
    return rows

def small(thumb):
    return re.sub(r"/3840px-", "/480px-", thumb)

def main():
    out = {}
    for cat, terms in SEARCHES.items():
        seen, rows = set(), []
        for t in terms:
            try:
                for r in search(t):
                    if r["title"] not in seen:
                        seen.add(r["title"]); rows.append(r)
            except Exception as e:
                print("search failed", t, e)
            time.sleep(4)
        rows = rows[:40]
        tiles = []
        for i, r in enumerate(rows):
            try:
                im = Image.open(io.BytesIO(get(small(r["thumb"]), raw=True))).convert("RGB")
                im.thumbnail((320, 200))
                tiles.append((i, im))
            except Exception as e:
                print("thumb failed", r["title"], e)
            time.sleep(1.5)
        cols = 5
        sheet = Image.new("RGB", (cols * 330, ((len(tiles) + cols - 1) // cols) * 225 + 10), (20, 20, 20))
        d = ImageDraw.Draw(sheet)
        for n, (i, im) in enumerate(tiles):
            x, y = (n % cols) * 330 + 5, (n // cols) * 225 + 5
            sheet.paste(im, (x, y))
            d.rectangle([x, y, x + 34, y + 22], fill=(0, 0, 0)); d.text((x + 4, y + 4), str(i), fill=(255, 255, 0))
        if tiles:
            sheet.save(os.path.join(OUT, f"sheet_{cat}.jpg"), quality=80)
        out[cat] = rows
        print(cat, len(rows), "candidates")
    with open(os.path.join(OUT, "candidates.json"), "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, indent=1)

if __name__ == "__main__":
    main()
