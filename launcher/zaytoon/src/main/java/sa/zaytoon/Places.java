package sa.zaytoon;

/** The cities offered for weather and prayer times; chosen with the remote, no typing. All in Saudi time (UTC+3). */
public final class Places {
    public static final class City {
        public final String id;
        public final String name;
        public final double lat;
        public final double lng;

        City(String id, String name, double lat, double lng) {
            this.id = id;
            this.name = name;
            this.lat = lat;
            this.lng = lng;
        }
    }

    public static final double UTC_OFFSET = 3;

    public static final City[] CITIES = {
            new City("khamis", "خميس مشيط", 18.3064, 42.7292),
            new City("abha", "أبها", 18.2164, 42.5053),
            new City("riyadh", "الرياض", 24.7136, 46.6753),
            new City("jeddah", "جدة", 21.4858, 39.1925),
            new City("makkah", "مكة المكرمة", 21.4225, 39.8262),
            new City("madinah", "المدينة المنورة", 24.4686, 39.6142),
            new City("dammam", "الدمام", 26.4207, 50.0888),
            new City("taif", "الطائف", 21.2703, 40.4158),
            new City("baha", "الباحة", 20.0129, 41.4677),
            new City("jazan", "جازان", 16.8892, 42.5511),
            new City("najran", "نجران", 17.5656, 44.2289),
            new City("tabuk", "تبوك", 28.3835, 36.5662),
            new City("hail", "حائل", 27.5114, 41.7208),
            new City("buraidah", "بريدة", 26.3260, 43.9750),
            new City("hofuf", "الأحساء", 25.3647, 49.5856),
            new City("alula", "العلا", 26.6085, 37.9232),
    };

    private Places() {
    }

    public static City city(String id) {
        for (City c : CITIES) if (c.id.equals(id)) return c;
        return CITIES[0];
    }
}
