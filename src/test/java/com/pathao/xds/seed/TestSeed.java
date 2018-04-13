package com.pathao.xds.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.pathao.xds.dto.ClusterDto;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Arrays.asList;

public class TestSeed {
    @Test
    public void seedConfig() throws Exception {
        Consul consul = Consul.builder()
//                .withHostAndPort(HostAndPort.fromParts("192.168.99.100", 32500))
                .withHostAndPort(HostAndPort.fromParts("localhost", 8500))
                .build();

        KeyValueClient keyValueClient = consul.keyValueClient();
        ObjectMapper objectMapper = new ObjectMapper();

        seedListener(keyValueClient);

        // zipkin
//        keyValueClient.putValue("clusters/zipkin", objectMapper
//                .writeValueAsString(new ClusterDto("zipkin", "zipkin", 9411)));

        // Front proxy
        keyValueClient.putValue("clusters/app1", objectMapper
                .writeValueAsString(new ClusterDto("app1", "app1", 80)));
        keyValueClient.putValue("clusters/app1_grpc", objectMapper
                .writeValueAsString(new ClusterDto("app1_grpc", "app1", 50052)));
        keyValueClient.putValue("clusters/app2", objectMapper
                .writeValueAsString(new ClusterDto("app2", "app1", 80)));

        // App1
        keyValueClient.putValue("clusters/local_app1", objectMapper
                .writeValueAsString(new ClusterDto("local_app1", "app1", 80)));
        keyValueClient.putValue("clusters/local_app1_grpc", objectMapper
                .writeValueAsString(new ClusterDto("local_app1_grpc", "app1", 50052)));
        keyValueClient.putValue("clusters/app2_grpc", objectMapper
                .writeValueAsString(new ClusterDto("app2", "app1", 80)));

        keyValueClient.putValue("version", "1");
        consul.destroy();
    }

    public void seedListener(KeyValueClient keyValueClient) throws IOException, URISyntaxException {
        List<String> list = asList(
                "fp_app1_http.json",
                "fp_app1_grpc.json"
//                "app1_local_grpc.json",
//                "app1_local_http.json"
        );
        for(String path : list) {
            String content = getContent(path);
            keyValueClient.putValue("listeners/" + path.replace(".json", ""), content);
        }
    }

    private String getContent(String path) throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(path).toURI())));
    }
}