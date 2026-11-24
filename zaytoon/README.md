# زيتون: واجهة تلفزيون عربية مبنية على CleanYourTV

فورك شخصي من [CleanYourTV](https://github.com/Gathin23/cleanyourtv) يحوّل اللانشر إلى واجهة عربية بهوية زيتية وذهبية،
للتلفزيون TCL C7L (Google TV 14). كل ما في المشروع الأصلي باقٍ كما هو؛ الإضافات هنا مفعّلة فقط من ثيم «زيتون».

## المرحلة الأولى: ما الذي تغيّر

| التغيير | أين |
|---|---|
| اسم حزمة مستقل `sa.zaytoon.home`، فيُثبَّت بجانب واجهة Google TV وبجانب CleanYourTV الأصلي دون أن يحل محل أي منهما | `launcher/gradle.properties` |
| خط عربي كامل: Noto Kufi Arabic (SIL OFL 1.1) بدل Outfit الذي لا يحوي حروفًا عربية، مقتطع إلى العربية واللاتينية الأساسية (١٤٩ كيلوبايت) | `launcher/app/src/main/res/font/`، `values-ar/fonts.xml` |
| اتجاه من اليمين لليسار للشبكة وشريط القوائم، كما في العبرية | `values-ar/bools.xml` |
| التاريخ الهجري بتقويم أم القرى من مكتبة ICU المدمجة في أندرويد، بأسماء الأشهر العربية | `Hijri.java`، المفتاح `hijri_date` |
| لون ذهبي للتحية والخط تحت الساعة وتوهج التركيز | `AccentText.java`، المفتاح `accent_text_color` |
| ثيم «زيتون»: خلفية زيتية `#141A12`، Dock زجاجي بشفافية ٧٠٪، أرقام هندية عبر `ar-SA-u-nu-arab` | `zaytoon/theme/config.json` |
| `tools/theme.sh` يقبل `LAUNCHER_PKG` ليرسل الثيم إلى حزمة الفورك | `tools/theme.sh`، `tools/theme_restart.py` |

### مفاتيح الثيم الجديدة

| المفتاح | النوع | المعنى |
|---|---|---|
| `hijri_date` | bool | التاريخ الهجري (أم القرى) في سطر التاريخ الإضافي، بلغة اللانشر وأرقامه. مطفأ ما لم يُفعَّل |
| `accent_text_color` | `#RRGGBB` | لون التحية والخط تحت الساعة وتوهج التركيز. بدونه يبقى الأبيض |

## البناء

يحتاج JDK 17 وAndroid SDK 34، ثم من مجلد `launcher/`:

```sh
./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

الناتج: `launcher/app/build/outputs/apk/release/app-release.apk`. ويبنيه أيضًا GitHub Actions في الفورك تلقائيًا (`ci.yml`).

## التجربة على التلفزيون بأمان

**لا تستخدم `launcher/build-and-install.sh`** في هذه المرحلة: هو يجعل اللانشر الشاشة الرئيسية ويعطّل واجهة Google TV.
الخطوات أدناه تثبّت التطبيق فقط وتجربه دون تغيير الشاشة الرئيسية. لا يُنفَّذ منها شيء إلا بموافقتك.

1. **نسخة احتياطية أولًا** (قراءة فقط، لا تغيّر شيئًا):
   ```sh
   mkdir -p backup
   TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'pm list packages' > backup/packages.txt
   TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'pm list packages -d' > backup/disabled.txt
   TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'getprop' > backup/getprop.txt
   TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME' > backup/home.txt
   ```
2. **التثبيت كتطبيق عادي:** `adb install app-release.apk`
3. **إرسال الثيم:** `LAUNCHER_PKG=sa.zaytoon.home TV_HOST=<tv ip> tools/theme.sh zaytoon/theme`
4. **الفتح للتجربة:** `adb shell am start -n sa.zaytoon.home/com.gathin.tvhome.MainActivity`
   ثم زر Home يعيدك إلى Google TV كالمعتاد. إن سألك التلفزيون عن تطبيق الشاشة الرئيسية، اختر Google TV.

### الرجوع

التطبيق تطبيق عادي ليس من النظام، فحذفه يعيد كل شيء كما كان:

```sh
adb uninstall sa.zaytoon.home
```

واجهة TCL وGoogle TV لا تُلمس في هذه المرحلة، ولا يُعطَّل أي تطبيق.

## الحقوق

- الكود: MIT كالمشروع الأصلي.
- Noto Kufi Arabic: © The Noto Project Authors، SIL OFL 1.1، النص في `launcher/FONT-LICENSE-OFL.txt`.
- خط الثلث للساعة: لم يُضمَّن بعد حتى يُتحقق من ترخيص مناسب.
