"""Round 3: for the shortlist, checks which 3840 px download works (Commons' 3840 thumbnail or the original) and
draws numbered contact sheets so the final picks are made by eye. Output in zaytoon/lab/out/."""
import io, json, os, time, urllib.error, urllib.parse, urllib.request
from PIL import Image, ImageDraw

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "out")
os.makedirs(OUT, exist_ok=True)
UA = {"User-Agent": "ZaytoonWallpaperProbe/1.1 (https://github.com/ielkun10/cleanyourtv; personal TV wallpapers)"}

def fetch(url, method="GET"):
    for attempt in range(6):
        try:
            req = urllib.request.Request(url, headers=UA, method=method)
            with urllib.request.urlopen(req, timeout=90) as r:
                return r.status, (r.read() if method == "GET" else b""), r.headers
        except urllib.error.HTTPError as e:
            if e.code == 429:
                time.sleep(10 * (attempt + 1)); continue
            return e.code, b"", {}
    return 429, b"", {}

def api(title, width):
    q = {"action": "query", "format": "json", "titles": title, "prop": "imageinfo",
         "iiprop": "url|size|sha1", "iiurlwidth": str(width)}
    status, body, _ = fetch("https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode(q))
    page = next(iter(json.loads(body)["query"]["pages"].values()))
    return page["imageinfo"][0]

def main():
    items = json.load(open(os.path.join(HERE, "shortlist.json"), encoding="utf-8"))
    tiles = []
    for n, it in enumerate(items):
        try:
            big = api(it["title"], 3840)
            st, _, h = fetch(big["thumburl"], "HEAD")
            it["thumb3840_status"] = st
            it["thumb3840"] = big["thumburl"]
            it["thumb3840_bytes"] = int(h.get("Content-Length", 0) or 0) if st == 200 else 0
            it["original"] = big["url"]
            it["original_size"] = big.get("size")
            small = api(it["title"], 330)
            st2, data, _ = fetch(small["thumburl"])
            if st2 == 200:
                im = Image.open(io.BytesIO(data)).convert("RGB")
                im.thumbnail((330, 220))
                tiles.append((n, im))
            else:
                print("small thumb", st2, it["title"])
        except Exception as e:
            print("failed", it["title"], e)
        time.sleep(1.2)
    cols = 6
    per = 36
    for s in range(0, len(tiles), per):
        chunk = tiles[s:s + per]
        sheet = Image.new("RGB", (cols * 340, ((len(chunk) + cols - 1) // cols) * 230 + 10), (15, 15, 15))
        d = ImageDraw.Draw(sheet)
        for k, (n, im) in enumerate(chunk):
            x, y = (k % cols) * 340 + 5, (k // cols) * 230 + 5
            sheet.paste(im, (x, y))
            d.rectangle([x, y, x + 40, y + 24], fill=(0, 0, 0))
            d.text((x + 6, y + 5), str(n), fill=(255, 230, 0))
        sheet.save(os.path.join(OUT, f"sheet_{s // per}.jpg"), quality=82)
    json.dump(items, open(os.path.join(OUT, "shortlist_checked.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)

if __name__ == "__main__":
    main()
