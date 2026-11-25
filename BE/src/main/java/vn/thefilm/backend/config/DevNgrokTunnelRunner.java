package vn.thefilm.backend.config;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Tu dong khoi chay Ngrok Tunnel phuc vu tiep nhan Webhook SePay (MBBank)
 * khi chay ung dung Spring Boot.
 */
@Component
public class DevNgrokTunnelRunner {

    private static final Logger log = LoggerFactory.getLogger(DevNgrokTunnelRunner.class);
    private static final String NGROK_API = "http://127.0.0.1:4040/api/tunnels";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ngrok.auto-start:true}")
    private boolean autoStart;

    @Value("${app.ngrok.domain:https://obstetrically-nontangible-rubi.ngrok-free.dev}")
    private String domain;

    @Value("${server.port:8080}")
    private int serverPort;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!autoStart) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                if (ensureNgrokTunnel()) {
                    log.info("[NGROK] Tunnel is active at: {} -> http://localhost:{}", domain, serverPort);
                    log.info("[NGROK] SePay Webhook is ready at: {}/api/v1/public/payments/webhook/sepay", domain);
                    return;
                }

                log.info("[NGROK] Auto-starting Ngrok tunnel: {} -> http://localhost:{}...", domain, serverPort);
                ProcessBuilder pb = new ProcessBuilder("ngrok", "http", String.valueOf(serverPort), "--url", domain);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Clean up ngrok process on Spring Boot shutdown
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                }));

                boolean active = false;
                for (int attempt = 0; attempt < 10 && !active; attempt++) {
                    Thread.sleep(1000);
                    active = ensureNgrokTunnel();
                }
                if (active) {
                    log.info("[NGROK] Tunnel connected successfully at: {}", domain);
                    log.info("[NGROK] SePay Webhook is ready at: {}/api/v1/public/payments/webhook/sepay", domain);
                } else {
                    log.info("[NGROK] Launched ngrok process in background.");
                }
            } catch (Exception e) {
                log.warn("[NGROK] Could not auto-start Ngrok tunnel: {}", e.getMessage());
            }
        });
    }

    private boolean ensureNgrokTunnel() {
        try {
            JsonNode tunnels = readTunnelList();
            JsonNode matchingTunnel = null;
            for (JsonNode tunnel : tunnels.path("tunnels")) {
                if (sameDomain(tunnel.path("public_url").asText(), domain)) {
                    matchingTunnel = tunnel;
                    if (sameTarget(tunnel.path("config").path("addr").asText(), targetAddress())) {
                        return true;
                    }
                }
            }
            if (matchingTunnel != null) {
                String oldTarget = matchingTunnel.path("config").path("addr").asText();
                String name = matchingTunnel.path("name").asText();
                log.warn("[NGROK] Tunnel {} points to {}, expected {}. Repointing it.", name, oldTarget, targetAddress());
                deleteTunnel(name);
            }
            if (createTunnelThroughAgent()) {
                JsonNode updated = readTunnelList();
                for (JsonNode tunnel : updated.path("tunnels")) {
                    if (sameDomain(tunnel.path("public_url").asText(), domain)
                            && sameTarget(tunnel.path("config").path("addr").asText(), targetAddress())) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private JsonNode readTunnelList() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(NGROK_API).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(1000);
        conn.setReadTimeout(1000);
        try (InputStream in = conn.getInputStream()) {
            return objectMapper.readTree(in);
        }
    }

    private void deleteTunnel(String name) throws Exception {
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) URI.create(NGROK_API + "/" + encodedName).toURL().openConnection();
        conn.setRequestMethod("DELETE");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);
        conn.getResponseCode();
        conn.disconnect();
    }

    private boolean createTunnelThroughAgent() throws Exception {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("name", "thefilm-backend");
        config.put("addr", targetAddress());
        config.put("proto", "http");
        config.put("inspect", true);
        config.put("domain", URI.create(domain.contains("://") ? domain : "https://" + domain).getHost());
        ArrayNode schemes = config.putArray("schemes");
        schemes.add("https");

        HttpURLConnection conn = (HttpURLConnection) URI.create(NGROK_API).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(5000);
        try (OutputStream out = conn.getOutputStream()) {
            objectMapper.writeValue(out, config);
        }
        int status = conn.getResponseCode();
        conn.disconnect();
        return status >= 200 && status < 300;
    }

    private String targetAddress() {
        return "http://localhost:" + serverPort;
    }

    private boolean sameTarget(String actual, String expected) {
        return actual != null && actual.replace("/", "").equalsIgnoreCase(expected.replace("/", ""));
    }

    private boolean sameDomain(String actual, String expected) {
        try {
            URI actualUri = URI.create(actual.contains("://") ? actual : "https://" + actual);
            URI expectedUri = URI.create(expected.contains("://") ? expected : "https://" + expected);
            return actualUri.getHost() != null && actualUri.getHost().equalsIgnoreCase(expectedUri.getHost());
        } catch (Exception ignored) {
            return false;
        }
    }
}
