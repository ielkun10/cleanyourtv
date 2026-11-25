package sa.zaytoon;

import java.util.HashMap;
import java.util.Map;

/** Clubs that can be followed (ESPN team ids and their league), and Arabic names for clubs and competitions. */
public final class Teams {
    public static final class Team {
        public final String id;
        public final String league;
        public final String name;

        Team(String id, String league, String name) {
            this.id = id;
            this.league = league;
            this.name = name;
        }

        public String crest() {
            return "https://a.espncdn.com/i/teamlogos/soccer/500/" + id + ".png";
        }
    }

    /** Followed by default: Manchester City, Barcelona and Chelsea. */
    public static final String DEFAULT = "382,83,363";

    public static final Team[] ALL = {
            new Team("382", "eng.1", "مانشستر سيتي"),
            new Team("83", "esp.1", "برشلونة"),
            new Team("363", "eng.1", "تشيلسي"),
            new Team("364", "eng.1", "ليفربول"),
            new Team("359", "eng.1", "آرسنال"),
            new Team("360", "eng.1", "مانشستر يونايتد"),
            new Team("367", "eng.1", "توتنهام"),
            new Team("362", "eng.1", "أستون فيلا"),
            new Team("361", "eng.1", "نيوكاسل"),
            new Team("86", "esp.1", "ريال مدريد"),
            new Team("1068", "esp.1", "أتلتيكو مدريد"),
            new Team("132", "ger.1", "بايرن ميونخ"),
            new Team("124", "ger.1", "بوروسيا دورتموند"),
            new Team("160", "fra.1", "باريس سان جيرمان"),
            new Team("111", "ita.1", "يوفنتوس"),
            new Team("110", "ita.1", "إنتر ميلان"),
            new Team("103", "ita.1", "ميلان"),
            new Team("929", "ksa.1", "الهلال"),
            new Team("817", "ksa.1", "النصر"),
            new Team("2276", "ksa.1", "الاتحاد"),
            new Team("8346", "ksa.1", "الأهلي"),
            new Team("793", "ksa.1", "الشباب"),
            new Team("8363", "ksa.1", "الاتفاق"),
            new Team("22022", "ksa.1", "القادسية"),
            new Team("18459", "ksa.1", "التعاون"),
            new Team("13033", "ksa.1", "الفتح"),
            new Team("21833", "ksa.1", "أبها"),
    };

    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Map<String, String> LEAGUES = new HashMap<>();

    static {
        String[] names = {
                "Arsenal", "آرسنال", "Brighton & Hove Albion", "برايتون", "Manchester City", "مانشستر سيتي",
                "Liverpool", "ليفربول", "AFC Bournemouth", "بورنموث", "Brentford", "برنتفورد", "Leeds United", "ليدز يونايتد",
                "Chelsea", "تشيلسي", "Everton", "إيفرتون", "Tottenham Hotspur", "توتنهام", "Hull City", "هال سيتي",
                "Aston Villa", "أستون فيلا", "Newcastle United", "نيوكاسل", "Coventry City", "كوفنتري سيتي", "Fulham", "فولهام",
                "Ipswich Town", "إبسويتش", "Nottingham Forest", "نوتنغهام فورست", "Sunderland", "سندرلاند",
                "Manchester United", "مانشستر يونايتد", "Crystal Palace", "كريستال بالاس", "West Ham United", "وست هام",
                "Wolverhampton Wanderers", "وولفرهامبتون", "Burnley", "بيرنلي", "Leicester City", "ليستر سيتي",
                "Southampton", "ساوثهامبتون", "Bradford City", "برادفورد سيتي",
                "Barcelona", "برشلونة", "Real Madrid", "ريال مدريد", "Atlético Madrid", "أتلتيكو مدريد", "Real Betis", "ريال بيتيس",
                "Real Sociedad", "ريال سوسيداد", "Sevilla", "إشبيلية", "Racing Santander", "راسينغ سانتاندير",
                "Athletic Club", "أتلتيك بلباو", "Alavés", "ألافيس", "Villarreal", "فياريال", "Espanyol", "إسبانيول",
                "Deportivo", "ديبورتيفو", "Osasuna", "أوساسونا", "Celta Vigo", "سيلتا فيغو", "Levante", "ليفانتي",
                "Rayo Vallecano", "رايو فاييكانو", "Valencia", "فالنسيا", "Getafe", "خيتافي", "Elche", "إلتشي", "Málaga", "ملقا",
                "Girona", "جيرونا", "Mallorca", "مايوركا", "Las Palmas", "لاس بالماس", "Real Oviedo", "ريال أوفييدو",
                "Paris Saint-Germain", "باريس سان جيرمان", "Como", "كومو", "Borussia Dortmund", "بوروسيا دورتموند",
                "Club Brugge", "كلوب بروج", "Sporting CP", "سبورتينغ لشبونة", "Galatasaray", "غلطة سراي",
                "Bayern Munich", "بايرن ميونخ", "VfB Stuttgart", "شتوتغارت", "Bodo/Glimt", "بودو غليمت",
                "Internazionale", "إنتر ميلان", "LASK Linz", "لاسك لينز", "FC Porto", "بورتو", "RB Leipzig", "لايبزيغ",
                "Fenerbahce", "فنربخشة", "AS Roma", "روما", "Shakhtar Donetsk", "شاختار دونيتسك", "Viking FK", "فايكنغ",
                "Napoli", "نابولي", "PSV Eindhoven", "آيندهوفن", "Sabah FK", "صباح", "AEK Athens", "أيك أثينا",
                "Feyenoord Rotterdam", "فينورد", "Lens", "لانس", "Slavia Prague", "سلافيا براغ", "Lille", "ليل",
                "Slovan Bratislava", "سلوفان براتيسلافا", "Juventus", "يوفنتوس", "AC Milan", "ميلان", "Benfica", "بنفيكا",
                "Ajax", "أياكس", "Bayer Leverkusen", "باير ليفركوزن", "Atalanta", "أتالانتا", "Monaco", "موناكو",
                "Al Hilal", "الهلال", "Al Qadsiah", "القادسية", "Al Nassr", "النصر", "Al Ahli", "الأهلي",
                "Al Ittihad", "الاتحاد", "Neom SC", "نيوم", "Al Ettifaq", "الاتفاق", "Al Diriyah", "الدرعية",
                "Al Khaleej", "الخليج", "Al Shabab", "الشباب", "Al Kholood", "الخلود", "Al Riyadh", "الرياض",
                "Al Fayha", "الفيحاء", "Al Hazem", "الحزم", "Al Faisaly", "الفيصلي", "Al Fateh", "الفتح",
                "Al Taawoun", "التعاون", "Abha", "أبها", "Al Okhdood", "الأخدود", "Damac", "ضمك", "Al Wehda", "الوحدة",
        };
        for (int i = 0; i + 1 < names.length; i += 2) NAMES.put(names[i], names[i + 1]);
        String[] leagues = {
                "eng.1", "الدوري الإنجليزي", "esp.1", "الدوري الإسباني", "ger.1", "الدوري الألماني",
                "ita.1", "الدوري الإيطالي", "fra.1", "الدوري الفرنسي", "ksa.1", "دوري روشن السعودي",
                "uefa.champions", "دوري أبطال أوروبا",
        };
        for (int i = 0; i + 1 < leagues.length; i += 2) LEAGUES.put(leagues[i], leagues[i + 1]);
    }

    private Teams() {
    }

    public static Team team(String id) {
        for (Team t : ALL) if (t.id.equals(id)) return t;
        return null;
    }

    /** The Arabic name of a club, or ESPN's own name for one not in the list. */
    public static String name(String espnName) {
        String n = NAMES.get(espnName);
        return n != null ? n : espnName;
    }

    public static String league(String slug) {
        String n = LEAGUES.get(slug);
        return n != null ? n : slug;
    }

    /** The Arabic name of a competition from ESPN's English one ("Premier League", "UEFA Champions League"). */
    public static String competition(String english) {
        if (english == null) return "";
        String e = english.toLowerCase(java.util.Locale.ROOT);
        if (e.contains("champions league")) return "دوري أبطال أوروبا";
        if (e.contains("europa league")) return "الدوري الأوروبي";
        if (e.contains("conference league")) return "دوري المؤتمر الأوروبي";
        if (e.contains("premier league") && e.contains("saudi") || e.contains("saudi pro") || e.contains("roshn")) return "دوري روشن السعودي";
        if (e.contains("premier league")) return "الدوري الإنجليزي";
        if (e.contains("laliga") || e.contains("la liga") || e.contains("spanish")) return "الدوري الإسباني";
        if (e.contains("bundesliga") || e.contains("german")) return "الدوري الألماني";
        if (e.contains("serie a") || e.contains("italian")) return "الدوري الإيطالي";
        if (e.contains("ligue 1") || e.contains("french")) return "الدوري الفرنسي";
        if (e.contains("carabao") || e.contains("league cup")) return "كأس الرابطة الإنجليزية";
        if (e.contains("fa cup")) return "كأس الاتحاد الإنجليزي";
        if (e.contains("copa del rey")) return "كأس ملك إسبانيا";
        if (e.contains("supercopa") || e.contains("super cup")) return "كأس السوبر";
        if (e.contains("king's cup") || e.contains("king cup")) return "كأس الملك";
        if (e.contains("club world cup")) return "كأس العالم للأندية";
        if (e.contains("afc champions")) return "دوري أبطال آسيا";
        if (e.contains("friendl")) return "مباراة ودية";
        return english;
    }
}
