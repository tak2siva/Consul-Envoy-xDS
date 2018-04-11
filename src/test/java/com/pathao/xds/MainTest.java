package com.pathao.xds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.pathao.xds.dto.ClusterDto;
import org.junit.Test;

public class MainTest {
    @Test
    public void seedConfig() throws Exception {
        Consul consul = Consul.builder()
                .withHostAndPort(HostAndPort.fromParts("192.168.99.100", 32500))
                .build();

        KeyValueClient keyValueClient = consul.keyValueClient();
        ObjectMapper objectMapper = new ObjectMapper();

        // zipkin
        keyValueClient.putValue("clusters/zipkin", objectMapper
                .writeValueAsString(new ClusterDto("zipkin", "zipkin", 9411)));

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
}