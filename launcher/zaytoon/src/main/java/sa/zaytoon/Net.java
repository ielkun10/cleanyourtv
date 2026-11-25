package sa.zaytoon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** The only network code in Zaytoon: HTTPS GETs to a fixed list of hosts, with timeouts and size caps. */
final class Net {
    /** Every host Zaytoon talks to. Anything else is refused before a connection is opened. */
    static final List<String> ALLOWED = Arrays.asList(
            "api.open-meteo.com",          // weather
            "site.api.espn.com",           // football fixtures, results, live scores, tables
            "a.espncdn.com",               // club crests
            "upload.wikimedia.org");       // wallpapers (Wikimedia Commons)

    private Net() {
    }

    static boolean allowed(String url) {
        try {
            URL u = new URL(url);
            return "https".equals(u.getProtocol()) && ALLOWED.contains(u.getHost());
        } catch (IOException e) {
            return false;
        }
    }

    private static HttpURLConnection open(String url) throws IOException {
        if (!allowed(url)) throw new IOException("host not allowed: " + url);
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent", "Zaytoon-TV/1.0 (personal Android TV launcher)");
        int code = c.getResponseCode();
        if (code != 200) {
            c.disconnect();
            throw new IOException("HTTP " + code + " for " + url);
        }
        return c;
    }

    static String text(String url, int maxBytes) throws IOException {
        HttpURLConnection c = open(url);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(in, out, maxBytes);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    /** Downloads to a temporary file next to target, then renames it, so a cut connection leaves no half file. */
    static void file(String url, File target, long maxBytes) throws IOException {
        HttpURLConnection c = open(url);
        File part = new File(target.getPath() + ".part");
        try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(part)) {
            copy(in, out, maxBytes);
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            part.delete();
            throw e;
        } finally {
            c.disconnect();
        }
        if (!part.renameTo(target)) {
            //noinspection ResultOfMethodCallIgnored
            part.delete();
            throw new IOException("rename failed: " + target);
        }
    }

    private static void copy(InputStream in, OutputStream out, long max) throws IOException {
        byte[] buf = new byte[16384];
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > max) throw new IOException("response larger than " + max + " bytes");
            out.write(buf, 0, n);
        }
    }
}
