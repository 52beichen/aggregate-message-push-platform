package dev.qingzhou.pushserver.config;

import dev.qingzhou.pushserver.grpc.PluginAuthInterceptor;
import dev.qingzhou.pushserver.grpc.PluginGatewayImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GrpcServerConfig {

    private final PluginGatewayImpl pluginGateway;
    private final PluginAuthInterceptor authInterceptor;

    @Value("${grpc.server.port:9090}")
    private int port;

    private Server server;

    @PostConstruct
    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(io.grpc.ServerInterceptors.intercept(pluginGateway, authInterceptor))
                .build()
                .start();
        log.info("gRPC Server started, listening on {}", port);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            log.info("Shutting down gRPC Server...");
            server.shutdown();
            try {
                if (!server.awaitTermination(30, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                    server.awaitTermination(5, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
            }
            log.info("gRPC Server stopped.");
        }
    }
}
