# زيتون: واجهة تلفزيون عربية مبنية على CleanYourTV

فورك شخصي من [CleanYourTV](https://github.com/Gathin23/cleanyourtv) يحوّل اللانشر إلى واجهة عربية بهوية زيتية وذهبية،
للتلفزيون TCL C7L مقاس ٧٥ بوصة (Android TV 14، البرنامج الثابت `UTT2.250416.001 AU02`).
هذا هو برنامج المثال المُختبر في `examples/tcl-65c7l-google-tv/`: الواجهة 1080p، والريموت يعمل بلا مساعد مفاتيح،
وتطبيق setupwraith يملك شبكة الواي فاي فلا يُلمس. كل وظائف المشروع الأصلي باقية.

## ما في التطبيق

| الميزة | التفاصيل | أين |
|---|---|---|
| حزمة مستقلة | `sa.zaytoon.home`، تُثبَّت بجانب واجهة Google TV ولا تحل محلها ولا تعطّل شيئًا | `launcher/gradle.properties` |
| الثيم مدمج | الخلفية الزيتية والذهبي والـ Dock الزجاجي والعربية والأرقام الهندية من أول تشغيل، بلا إرسال ثيم | `launcher/zaytoon/src/main/assets/zaytoon/config.json`، `ThemeFile.java` |
| ساعة زجاجية كبيرة | خط Katibeh المستوحى من الثلث (OFL)، ثلاثة أحجام، على لوح زجاجي، في يمين الشاشة | `Zaytoon.java` |
| التاريخ الهجري | تقويم أم القرى بأسماء الأشهر العربية | `Hijri.java` |
| بطاقات تحت الساعة | الطقس، الصلاة القادمة مع العدّ التنازلي، مباراة النادي (مباشرة أو قادمة) | `HomeStrip.java` |
| لوحة زيتون | زر غصن الزيتون في الشريط العلوي: الطقس، الصلاة، الرياضة، الخلفيات، المظهر، حول. كلها بالأسهم وOK ورجوع | `ZaytoonActivity.java` |
| الطقس | Open-Meteo مجانًا بلا مفتاح، ١٦ مدينة سعودية، خميس مشيط افتراضيًا، تحديث كل ١٥ أو ٣٠ أو ٦٠ دقيقة | `Weather.java`، `Hub.java` |
| مواقيت الصلاة | تُحسب على الجهاز دون إنترنت؛ أم القرى افتراضيًا (العشاء +١٢٠ د في رمضان)، وأربع طرق أخرى، والعصر الحنفي | `PrayerTimes.java` |
| الرياضة | خدمة ESPN العامة المجانية بلا مفتاح: المباريات القادمة، المباشر (كل دقيقة أثناء مباريات أنديتك فقط)، آخر النتائج، الترتيب، الشعارات. افتراضيًا: مانشستر سيتي، برشلونة، تشيلسي | `Football.java`، `Teams.java` |
| خلفيات 4K | جبال وصحارى ومعالم سعودية من ويكيميديا كومنز بتراخيص حرة، تُنزَّل مرة واحدة (ليست داخل APK)، مع التدوير والتعتيم واسم المصوّر، واستيراد صورك من مجلد `Zaytoon` | `Wallpapers.java`، `assets/zaytoon/wallpapers.json` |
| المظهر | لون اللمسات، لون الخلفية، شفافية الـ Dock، حجم الأيقونات، حجم الساعة وخطها، إظهار البطاقات، الساعة عند الخمول | لوحة زيتون ← المظهر |

## الإنترنت والخصوصية

اللانشر الأصلي بلا إنترنت. زيتون يضيف صلاحية `INTERNET` لأنك طلبت الطقس والرياضة والخلفيات، ويقيّدها بقائمة ثابتة في
`Net.java`: `api.open-meteo.com` و`site.api.espn.com` و`a.espncdn.com` و`upload.wikimedia.org`، عبر HTTPS فقط، ولا
يتصل بشيء إلا والشاشة الرئيسية أو اللوحة مفتوحة. لا حساب ولا مفتاح ولا اشتراك ولا تحليلات.

## حدود المصادر المجانية

- **الرياضة:** API-Football يحتاج مفتاحًا وتسجيلًا، وخطته المجانية لا تضمن الموسم الحالي، وTheSportsDB المجاني مقيّد بنتيجة واحدة
  وبلا مباشر. لذلك اخترنا خدمة ESPN العامة: مجانية وبلا مفتاح وتغطي الموسم الحالي (تحققنا منها لمانشستر سيتي وبرشلونة وتشيلسي في
  ٢٥ نوفمبر ٢٠٢٦). هي ليست واجهة رسمية موثّقة، فقد تتغير يومًا؛ عندها تعرض اللوحة آخر بيانات محفوظة وتقول إن المصدر تعذّر.
- **الطقس:** Open-Meteo مجاني للاستخدام غير التجاري، رخصة بياناته CC BY 4.0.
- **الخلفيات:** ويكيميديا كومنز، CC0 وCC BY وCC BY-SA؛ اسم المصوّر والرخصة تحت الأزرار ومحفوظة في `wallpapers.json`.

## البناء والاختبار

يبنيه GitHub Actions في الفورك (`.github/workflows/zaytoon-apk.yml`): اختبارات الوحدة وlint والبناء، ثم يثبّته على محاكي
Android TV 14 بدقة 1920×1080 ويتنقل فيه بأزرار الريموت ويلتقط الشاشات، ثم يضع الـ APK واللقطات في فرع `zaytoon/build-output`.

محليًا (JDK 17 وAndroid SDK 34)، من مجلد `launcher/`:

```sh
./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

## التثبيت على التلفزيون

1. **نسخة احتياطية أولًا** (قراءة فقط):
   ```sh
   mkdir -p backup
   TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'pm list packages' > backup/packages.txt
   TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'pm list packages -d' > backup/disabled.txt
   TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME' > backup/home.txt
   ```
2. **إن كانت نسخة سابقة من زيتون مثبتة:** `adb uninstall sa.zaytoon.home` (كل بناء من GitHub موقّع بمفتاح مختلف).
3. **التثبيت:** `adb install zaytoon-launcher.apk`، أو انسخه على فلاشة وثبّته بمدير ملفات.
4. **الفتح:** من قائمة التطبيقات في Google TV اختر «زيتون»، أو `adb shell am start -n sa.zaytoon.home/com.gathin.tvhome.MainActivity`.
   زر Home يعيدك إلى Google TV كالمعتاد.
5. **اختياري، لاحقًا:** لجعله الشاشة الرئيسية اتبع `docs/launcher.md`، ولا تستخدم `launcher/build-and-install.sh` قبل أن تجربه أيامًا.

**الرجوع:** `adb uninstall sa.zaytoon.home`، أو الإعدادات ← التطبيقات ← زيتون ← إلغاء التثبيت. واجهة TCL وGoogle TV لا تُلمس.

## الحقوق

- الكود: MIT كالمشروع الأصلي.
- Noto Kufi Arabic: © The Noto Project Authors، وKatibeh: © KB-Studio وLasse Fister وEduardo Tunni؛ كلاهما SIL OFL 1.1
  (`launcher/FONT-LICENSE-OFL.txt` و`launcher/zaytoon/FONT-LICENSE-KATIBEH-OFL.txt`).
- الصور: لأصحابها بتراخيصها المذكورة في `wallpapers.json`. شعارات الأندية ملك أنديتها، تُعرض للاستخدام الشخصي.
