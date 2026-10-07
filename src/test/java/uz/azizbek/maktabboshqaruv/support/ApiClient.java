package uz.azizbek.maktabboshqaruv.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Tiny HTTP client for integration tests: JSON in, (status, JSON) out, with a Bearer token. */
public class ApiClient {

    public record Response(int status, String body, HttpResponse<byte[]> raw) {
        public JsonNode json() {
            return MAPPER.readTree(body);
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;
    private String token;

    public ApiClient(int port) {
        this.base = "http://localhost:" + port;
    }

    public ApiClient login(String username, String password) throws Exception {
        Response r = send("POST", "/api/auth/login", "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        if (r.status() != 200) throw new IllegalStateException("login " + r.status() + ": " + r.body());
        token = r.body().trim();
        return this;
    }

    public Response get(String path) throws Exception {
        return send("GET", path, null);
    }

    public Response post(String path, Object body) throws Exception {
        return send("POST", path, body instanceof String s ? s : MAPPER.writeValueAsString(body));
    }

    public Response put(String path, Object body) throws Exception {
        return send("PUT", path, body == null ? null : body instanceof String s ? s : MAPPER.writeValueAsString(body));
    }

    public Response delete(String path) throws Exception {
        return send("DELETE", path, null);
    }

    /** One part of a multipart request: a JSON part ({@code fileName} null) or a file. */
    public record Part(String name, String fileName, String contentType, byte[] bytes) {
        public static Part json(String name, Object value) {
            return new Part(name, null, "application/json", MAPPER.writeValueAsString(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        public static Part file(String name, String fileName, String contentType, byte[] bytes) {
            return new Part(name, fileName, contentType, bytes);
        }
    }

    public Response multipart(String path, java.util.List<Part> parts) throws Exception {
        String boundary = "----test" + System.nanoTime();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (Part p : parts) {
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + p.name() + "\""
                    + (p.fileName() != null ? "; filename=\"" + p.fileName() + "\"" : "")
                    + "\r\nContent-Type: " + p.contentType() + "\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.write(p.bytes());
            out.write("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        out.write(("--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()));
        if (token != null) b.header("Authorization", "Bearer " + token);
        HttpResponse<byte[]> r = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        return new Response(r.statusCode(), new String(r.body(), java.nio.charset.StandardCharsets.UTF_8), r);
    }

    public Response send(String method, String path, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (json != null) b.header("Content-Type", "application/json");
        b.method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        HttpResponse<byte[]> r = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        return new Response(r.statusCode(), new String(r.body(), java.nio.charset.StandardCharsets.UTF_8), r);
    }
}
