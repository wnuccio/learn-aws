package org.example.fargate;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class GreeterServer {

    private static final Logger log = LoggerFactory.getLogger(GreeterServer.class);

    private static final int PORT = 8080;

    private static final Greeter greeter = new Greeter();

    public static void main(String[] args) throws IOException {
        // binds all interfaces, so the server is reachable from outside the container
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/hello", GreeterServer::handleHello);
        server.start();
        log.info("listening on port {}", PORT);
    }

    private static void handleHello(HttpExchange exchange) throws IOException {
        String name = queryParameter(exchange.getRequestURI().getRawQuery(), "name");
        String greeting = greeter.greet(name);
        log.info("greeted name={}", name);

        byte[] body = greeting.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String queryParameter(String query, String key) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}