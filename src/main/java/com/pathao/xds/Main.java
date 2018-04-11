package com.pathao.xds;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import io.envoyproxy.controlplane.cache.SimpleCache;
import io.envoyproxy.controlplane.cache.Snapshot;
import io.envoyproxy.controlplane.server.DiscoveryServer;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

public class Main {
    public static String GROUP = "Global";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static SimpleCache<String> cache = new SimpleCache<>(true, node -> GROUP);

    private static String consulHost;
    private static String consulPort;

    public static void main(String[] args) throws IOException, InterruptedException {
        initConfig();
        DiscoveryServer discoveryServer = new DiscoveryServer(cache);

        ServerBuilder builder = NettyServerBuilder.forPort(12345)
                .addService(discoveryServer.getAggregatedDiscoveryServiceImpl())
                .addService(discoveryServer.getClusterDiscoveryServiceImpl())
                .addService(discoveryServer.getEndpointDiscoveryServiceImpl())
                .addService(discoveryServer.getListenerDiscoveryServiceImpl())
                .addService(discoveryServer.getRouteDiscoveryServiceImpl());

        Server server = builder.build();

        server.start();

        System.out.println("Server has started on port " + server.getPort());
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        scheduler.scheduleAtFixedRate(fetchAndUpdateConfig(), 0, 10, TimeUnit.SECONDS);

        server.awaitTermination();
    }

    private static void initConfig() {
        String fileName = System.getProperty("prop");
        System.out.println("Property file: " + fileName);
        InputStream input = null;

        try {
            input = new FileInputStream(fileName);
            Properties prop = new Properties();

            prop.load(input);
            consulHost = prop.getProperty("host");
            consulPort = prop.getProperty("port");
            System.out.println("Consul address " + consulHost + ":" + consulPort);
        } catch (FileNotFoundException e) {
            System.out.println("Error reading properties file");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Runnable fetchAndUpdateConfig() {
        return () -> {
            Consul consul = getConsulClient();
            KeyValueClient keyValueClient = consul.keyValueClient();

            String version = getVersion(keyValueClient);
            ClusterDao clusterDao = new ClusterDao(keyValueClient);

            System.out.println("Updating with version " + version);

            cache.setSnapshot(
                    GROUP,
                    Snapshot.create(
                            Collections.unmodifiableList(clusterDao.getClusters()),
                            Collections.unmodifiableList(emptyList()),
                            Collections.unmodifiableList(emptyList()),
                            Collections.unmodifiableList(emptyList()),
                            version
                    )
            );

            consul.destroy();
        };
    }

    private static Consul getConsulClient() {
        return Consul.builder()
                .withHostAndPort(HostAndPort.fromParts("localhost", 8500))
                .build();
    }

    public static String getVersion(KeyValueClient keyValueClient) {
        return keyValueClient.getValueAsString("version").get();
    }
}
