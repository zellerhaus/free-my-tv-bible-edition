package io.freemytv;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The Verse of the Day: one small JSON document a day, read from the feed in config.properties (votd_url), which a
 * server fills from YouVersion and keeps the keys. The TV only reads it: once a day, again after midnight, and an hour
 * later if the feed didn't answer; yesterday's verse stays on screen until today's arrives. Cached so the screen opens
 * with it. "asset:<name>" reads a sample from the app's assets instead (for building without the feed).
 *
 * The document: date (YYYY-MM-DD), reference ("John 3:16"), text, bible ("NIV"), copyright (the version's line, shown
 * as plain text wherever the verse is), context (two sentences about who, to whom and why) and youversion_url.
 */
final class Verse {
    static final class Day {
        String date = "", reference = "", text = "", bible = "", copyright = "", context = "";
    }

    interface Listener { void onVerse(); }

    private static final long RETRY_MS = 60 * 60_000L, FAIL_MS = 10 * 60_000L;
    private static Verse instance;

    static synchronized Verse get(Context c) {
        if (instance == null) instance = new Verse(c.getApplicationContext());
        return instance;
    }

    /** A feed is set: the setting shows up and the verse can be fetched. */
    static boolean configured() {
        return !Config.VOTD_URL.trim().isEmpty();
    }

    /** The verse is on (as a tab or as a drawer card; the screensaver shows it either way). */
    static boolean on() {
        return configured() && !Prefs.verseMode().equals("off");
    }

    /** First tab of the home screen: the verse on the left, its context on the right. */
    static boolean tab() {
        return configured() && Prefs.verseMode().equals("tab");
    }

    /** A card in the bottom drawer, beside the weather and the prices. */
    static boolean card() {
        return configured() && Prefs.verseMode().equals("card");
    }

    /** The credit shown with the verse: where the verse and its context come from. */
    static String credit() {
        return Ui.tr("Versículo do dia pela YouVersion Platform · Contexto por Psalmlog",
                "Verse of the Day from the YouVersion Platform · Context by Psalmlog");
    }

    private final Context context;
    private final SharedPreferences cache;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private volatile boolean fetching;
    private volatile long failedAt, triedAt;
    /** Today's verse (or the last one that arrived); null before the first fetch. */
    volatile Day day;
    volatile long fetchedAt;

    private Verse(Context c) {
        context = c;
        cache = c.getSharedPreferences("verse", Context.MODE_PRIVATE);
        String saved = cache.getString("day", null);
        if (saved != null) {
            try {
                day = parse(new JSONObject(saved));
                fetchedAt = cache.getLong("at", 0);
            } catch (Exception ignored) {}
        }
    }

    void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    void removeListener(Listener l) { listeners.remove(l); }

    /** Fetches today's verse if the one on screen isn't today's (the screens call this when shown and every minute). */
    void refreshIfStale() {
        if (!on() || fetching) return;
        long now = System.currentTimeMillis();
        Day d = day;
        if (d != null && today().equals(d.date)) return;
        if (failedAt != 0 && now >= failedAt && now - failedAt < FAIL_MS) return;
        // The feed answered with yesterday's (not generated yet): don't hammer it, ask again in an hour.
        if (d != null && triedAt != 0 && now >= triedAt && now - triedAt < RETRY_MS) return;
        fetching = true;
        io.execute(() -> {
            try {
                Day fresh = parse(new JSONObject(fetch(Config.VOTD_URL.trim())));
                triedAt = System.currentTimeMillis();
                if (fresh.date.isEmpty()) fresh.date = today();
                Day current = day;
                if (current == null || !current.date.equals(fresh.date) || !current.text.equals(fresh.text)) {
                    day = fresh;
                    fetchedAt = System.currentTimeMillis();
                    cache.edit().putString("day", toJson(fresh)).putLong("at", fetchedAt).apply();
                    notifyListeners();
                }
            } catch (Exception e) {
                failedAt = System.currentTimeMillis();
                AppLog.w("verse: " + e.getMessage());
            } finally {
                fetching = false;
            }
        });
    }

    private void notifyListeners() {
        main.post(() -> { for (Listener l : new ArrayList<>(listeners)) l.onVerse(); });
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** The verse must come with its reference, its text and the version's copyright line, or it isn't shown. */
    private static Day parse(JSONObject o) throws IOException {
        Day d = new Day();
        d.date = o.optString("date", "").trim();
        d.reference = o.optString("reference", "").trim();
        d.text = o.optString("text", "").trim().replaceAll("\\s+", " ");
        d.bible = o.optString("bible", "").trim();
        // The copyright line can come with line breaks: one line of text, laid out by the screen that shows it.
        d.copyright = o.optString("copyright", "").trim().replaceAll("\\s+", " ");
        d.context = o.optString("context", "").trim().replaceAll("\\s+", " ");
        if (d.reference.isEmpty() || d.text.isEmpty()) throw new IOException("the feed has no verse");
        if (d.copyright.isEmpty()) throw new IOException("the feed has no copyright line");
        return d;
    }

    private static String toJson(Day d) {
        try {
            return new JSONObject().put("date", d.date).put("reference", d.reference).put("text", d.text)
                    .put("bible", d.bible).put("copyright", d.copyright).put("context", d.context).toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private String fetch(String url) throws IOException {
        if (url.startsWith("asset:")) {
            try (InputStream in = context.getAssets().open(url.substring(6))) {
                return read(in);
            }
        }
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setInstanceFollowRedirects(true);   // "/today" sends to the dated file
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "FreeMyTV");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            try (InputStream in = c.getInputStream()) {
                return read(in);
            }
        } finally {
            c.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
            if (out.size() > 64 * 1024) throw new IOException("the feed is too large");
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }
}
