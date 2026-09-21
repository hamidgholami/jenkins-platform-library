/**
 * Copyright 2026 Hamid Gholami
 * SPDX-License-Identifier: Apache-2.0
 * */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Read-only, authenticated smart HTTP transport for disposable Git fixtures.
 * */
final class GitServer {
    public static void main(final String[] arguments) throws IOException {
        final String password = System.getenv("TEST_GIT_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Fixture authentication is required");
        }
        final String authorization = "Basic " + Base64.getEncoder().encodeToString(
                ("reader:" + password).getBytes(StandardCharsets.UTF_8));
        final HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/", exchange -> serve(exchange, authorization));
        server.start();
    }

    private static void serve(final HttpExchange exchange, final String authorization) throws IOException {
        try (exchange) {
            if (!authorization.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=fixture");
                exchange.sendResponseHeaders(401, -1);
                return;
            }
            final String path = exchange.getRequestURI().getPath();
            final String query = exchange.getRequestURI().getRawQuery();
            if (!path.matches("/(library|source)\\.git/(info/refs|git-upload-pack)") ||
                    (query != null && !query.equals("service=git-upload-pack"))) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }
            final ProcessBuilder builder = new ProcessBuilder("git",
                    "-c", "safe.directory=/fixtures/library.git",
                    "-c", "safe.directory=/fixtures/source.git", "http-backend");
            final byte[] request = exchange.getRequestBody().readAllBytes();
            final Map<String, String> environment = builder.environment();
            environment.put("GIT_PROJECT_ROOT", "/fixtures");
            environment.put("GIT_HTTP_EXPORT_ALL", "1");
            environment.put("PATH_INFO", path);
            environment.put("QUERY_STRING", query == null ? "" : query);
            environment.put("REQUEST_METHOD", exchange.getRequestMethod());
            environment.put("REMOTE_USER", "reader");
            environment.put("CONTENT_LENGTH", Integer.toString(request.length));
            environment.put("CONTENT_TYPE", exchange.getRequestHeaders().getFirst("Content-Type") == null
                    ? "" : exchange.getRequestHeaders().getFirst("Content-Type"));
            final String protocol = exchange.getRequestHeaders().getFirst("Git-Protocol");
            if (protocol != null) {
                environment.put("GIT_PROTOCOL", protocol);
            }
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            final Process backend = builder.start();
            backend.getOutputStream().write(request);
            backend.getOutputStream().close();
            final byte[] response = backend.getInputStream().readAllBytes();
            final String wire = new String(response, StandardCharsets.ISO_8859_1);
            final int boundary = wire.indexOf("\r\n\r\n");
            if (boundary < 0) {
                exchange.sendResponseHeaders(502, -1);
                return;
            }
            int status = 200;
            for (final String header : wire.substring(0, boundary).split("\r\n")) {
                final int colon = header.indexOf(':');
                if (colon > 0) {
                    final String key = header.substring(0, colon);
                    final String value = header.substring(colon + 1).trim();
                    if (key.equalsIgnoreCase("Status")) {
                        status = Integer.parseInt(value.substring(0, 3));
                    } else {
                        exchange.getResponseHeaders().add(key, value);
                    }
                }
            }
            final byte[] body = Arrays.copyOfRange(response, boundary + 4, response.length);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }
    }
}
