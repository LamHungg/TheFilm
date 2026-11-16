package vn.thefilm.backend.config;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
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
                if (isNgrokActive()) {
                    log.info("[NGROK] Tunnel is already active at: {} -> http://localhost:{}", domain, serverPort);
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

                Thread.sleep(2000);
                if (isNgrokActive()) {
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

    private boolean isNgrokActive() {
        try {
            URL url = URI.create("http://127.0.0.1:4040/api/tunnels").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            int code = conn.getResponseCode();
            if (code == 200) {
                try (InputStream in = conn.getInputStream()) {
                    String body = new String(in.readAllBytes());
                    return body.contains("obstetrically-nontangible-rubi.ngrok-free.dev");
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
