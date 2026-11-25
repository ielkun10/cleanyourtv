"""Zaytoon data probe, run by .github/workflows/zaytoon-probe.yml (the container that writes the code has no
internet). Collects 4K wallpaper candidates from Wikimedia Commons with their licences, and samples of the free
weather and football feeds, into zaytoon/lab/out/ which the workflow pushes to the zaytoon/lab-results branch."""
import json, os, sys, time, urllib.parse, urllib.request

OUT = os.path.join(os.path.dirname(__file__), "out")
os.makedirs(OUT, exist_ok=True)
UA = {"User-Agent": "ZaytoonProbe/1.0 (personal TV launcher build check)"}

def get(url, raw=False):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=40) as r:
        data = r.read()
    return data if raw else json.loads(data)

def save(name, obj):
    with open(os.path.join(OUT, name), "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)

SEARCHES = {
    "makkah": ["Masjid al-Haram", "Kaaba Mecca", "Abraj Al Bait Mecca night", "Mecca city view"],
    "madinah": ["Al-Masjid an-Nabawi", "Prophet's Mosque Medina umbrellas", "Quba Mosque", "Medina city"],
    "alula": ["Hegra Mada'in Salih", "AlUla Elephant Rock", "AlUla landscape", "Maraya AlUla"],
    "asir": ["Abha", "Al Soudah", "Asir mountains", "Rijal Almaa", "Khamis Mushait"],
    "hijaz": ["Taif mountains", "Al Hada road Taif", "Hejaz mountains"],
    "najd": ["Edge of the World Saudi Arabia", "Tuwaiq escarpment", "Riyadh skyline", "Diriyah At-Turaif"],
    "dunes": ["Rub' al Khali dunes", "Saudi Arabia desert dunes", "Nafud desert"],
    "redsea": ["Red Sea Saudi Arabia coast", "Farasan Islands", "Jeddah corniche", "Umluj"],
}
OK_LIC = ("cc0", "cc-by", "cc by", "public domain", "pd")

def commons(term):
    q = {"action": "query", "format": "json", "generator": "search", "gsrnamespace": "6",
         "gsrsearch": f"{term} filew:>3839 filetype:bitmap", "gsrlimit": "25",
         "prop": "imageinfo", "iiprop": "url|size|sha1|extmetadata|mime", "iiurlwidth": "3840"}
    data = get("https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode(q))
    rows = []
    for p in (data.get("query", {}).get("pages", {}) or {}).values():
        ii = (p.get("imageinfo") or [{}])[0]
        md = ii.get("extmetadata", {})
        lic = (md.get("LicenseShortName", {}).get("value") or "")
        if not any(k in lic.lower() for k in OK_LIC):
            continue
        if ii.get("width", 0) < 3840 or ii.get("width", 0) < ii.get("height", 0):
            continue
        rows.append({
            "title": p["title"], "w": ii.get("width"), "h": ii.get("height"), "mime": ii.get("mime"),
            "license": lic, "license_url": md.get("LicenseUrl", {}).get("value"),
            "artist": md.get("Artist", {}).get("value"), "credit": md.get("Credit", {}).get("value"),
            "desc": (md.get("ImageDescription", {}).get("value") or "")[:300],
            "page": ii.get("descriptionurl"), "url": ii.get("url"), "thumb": ii.get("thumburl"),
            "thumb_w": ii.get("thumbwidth"), "thumb_h": ii.get("thumbheight"), "sha1": ii.get("sha1"),
        })
    return rows

def main():
    cands = {}
    for cat, terms in SEARCHES.items():
        seen, rows = set(), []
        for t in terms:
            try:
                for r in commons(t):
                    if r["title"] not in seen:
                        seen.add(r["title"]); r["term"] = t; rows.append(r)
            except Exception as e:
                rows.append({"error": f"{t}: {e}"})
            time.sleep(1)
        cands[cat] = rows
    save("commons.json", cands)

    feeds = {
        "espn_teams": "https://site.api.espn.com/apis/site/v2/sports/soccer/ksa.1/teams",
        "espn_scoreboard": "https://site.api.espn.com/apis/site/v2/sports/soccer/ksa.1/scoreboard",
        "espn_standings": "https://site.api.espn.com/apis/v2/sports/soccer/ksa.1/standings",
        "espn_leagues": "https://site.api.espn.com/apis/site/v2/sports/soccer/leagues",
        "meteo": "https://api.open-meteo.com/v1/forecast?latitude=18.3&longitude=42.73&current=temperature_2m,weather_code,is_day,relative_humidity_2m,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset&timezone=auto&forecast_days=4",
        "geo_ar": "https://geocoding-api.open-meteo.com/v1/search?name=" + urllib.parse.quote("أبها") + "&count=5&language=ar&format=json",
        "aladhan_khamis": "https://api.aladhan.com/v1/timings/25-11-2026?latitude=18.3&longitude=42.73&method=4",
        "aladhan_makkah": "https://api.aladhan.com/v1/timings/25-11-2026?latitude=21.4225&longitude=39.8262&method=4",
        "aladhan_riyadh": "https://api.aladhan.com/v1/timings/01-06-2026?latitude=24.7136&longitude=46.6753&method=4",
    }
    for name, url in feeds.items():
        try:
            save(name + ".json", get(url))
        except Exception as e:
            save(name + ".json", {"error": str(e)})
    try:
        teams = json.load(open(os.path.join(OUT, "espn_teams.json")))
        team = teams["sports"][0]["leagues"][0]["teams"][0]["team"]
        save("espn_team_schedule.json", get(f"https://site.api.espn.com/apis/site/v2/sports/soccer/ksa.1/teams/{team['id']}/schedule"))
        save("espn_team_schedule_fixture.json", get(f"https://site.api.espn.com/apis/site/v2/sports/soccer/ksa.1/teams/{team['id']}/schedule?fixture=true"))
    except Exception as e:
        save("espn_team_schedule.json", {"error": str(e)})

if __name__ == "__main__":
    sys.exit(main())
