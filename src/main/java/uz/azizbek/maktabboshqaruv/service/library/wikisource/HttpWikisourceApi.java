package uz.azizbek.maktabboshqaruv.service.library.wikisource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MediaWiki API over HTTPS, as Wikimedia asks of bots: a User-Agent naming the project, at most one
 * request per second (for the whole application), and a 30-second timeout. Called only from the
 * backend, only while an admin browses or imports.
 */
@Component
public class HttpWikisourceApi implements WikisourceApi {

    private static final Logger log = LoggerFactory.getLogger(HttpWikisourceApi.class);
    static final String USER_AGENT =
            "MaktabBoshqaruv/1.0 (maktab elektron kutubxonasi; https://github.com/Doniyorov-Aziz/maktab-boshqaruv)";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final long MIN_GAP_MS = 1000;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private final ObjectMapper json = new ObjectMapper();
    private long lastRequest;

    @Override
    public String wikitext(String host, String title) {
        JsonNode root = call(host, Map.of("action", "parse", "page", title, "prop", "wikitext", "redirects", "1"));
        if (root.has("error")) return null;
        JsonNode text = root.path("parse").path("wikitext");
        return text.isMissingNode() || text.isNull() ? null : text.asString();
    }

    @Override
    public List<String> search(String host, String query, int namespace, int limit) {
        JsonNode root = call(host, Map.of("action", "query", "list", "search", "srsearch", query,
                "srnamespace", String.valueOf(namespace), "srlimit", String.valueOf(limit)));
        List<String> titles = new ArrayList<>();
        for (JsonNode n : root.path("query").path("search")) titles.add(n.path("title").asString());
        return titles;
    }

    @Override
    public List<String> pagesWithPrefix(String host, String prefix, int limit) {
        JsonNode root = call(host, Map.of("action", "query", "list", "allpages", "apprefix", prefix,
                "aplimit", String.valueOf(limit)));
        List<String> titles = new ArrayList<>();
        for (JsonNode n : root.path("query").path("allpages")) titles.add(n.path("title").asString());
        return titles;
    }

    private JsonNode call(String host, Map<String, String> params) {
        Map<String, String> all = new LinkedHashMap<>(params);
        all.put("format", "json");
        all.put("formatversion", "2");
        String query = all.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        URI uri = URI.create("https://" + host + "/w/api.php?" + query);
        pace();
        try {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(uri).timeout(TIMEOUT)
                    .header("User-Agent", USER_AGENT).header("Accept", "application/json").GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Wikisource javob bermadi (HTTP " + r.statusCode() + ")");
            }
            return json.readTree(r.body());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Wikisource so'rovi to'xtatildi");
        } catch (Exception e) {
            log.info("Wikisource so'rovi bajarilmadi ({}): {}", host, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Wikisource'ga ulanib bo'lmadi: " + e.getMessage());
        }
    }

    /** At most one request per second, whoever asks. */
    private synchronized void pace() {
        long wait = lastRequest + MIN_GAP_MS - System.currentTimeMillis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequest = System.currentTimeMillis();
    }
}
