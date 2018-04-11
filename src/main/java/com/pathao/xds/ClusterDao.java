package com.pathao.xds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Duration;
import com.orbitz.consul.KeyValueClient;
import com.pathao.xds.dto.ClusterDto;
import envoy.api.v2.Cds;
import envoy.api.v2.core.AddressOuterClass;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ClusterDao {
    private KeyValueClient keyValueClient;

    public ClusterDao(KeyValueClient keyValueClient) {
        this.keyValueClient = keyValueClient;
    }

    public List<Cds.Cluster> getClusters() {
        ObjectMapper mapper = new ObjectMapper();

        List<ClusterDto> clusterDtoList = keyValueClient.getKeys("clusters").stream().map(key -> {
            String name = key.replace("clusters/", "");
            Optional<String> valueAsString = keyValueClient.getValueAsString(key);
            ClusterDto clusterDto = null;

            if (valueAsString.isPresent()) {
                try {
                    clusterDto = mapper.readValue(valueAsString.get(), ClusterDto.class);
                    clusterDto.name = name;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return clusterDto;
        }).collect(toList());

        return clusterDtoList.stream().map(clusterDto -> {
            return Cds.Cluster.newBuilder()
                    .setName(clusterDto.name)
                    .setConnectTimeout(Duration.newBuilder().setSeconds(5))
                    .setType(Cds.Cluster.DiscoveryType.STRICT_DNS)
                    .addHosts(AddressOuterClass.Address
                            .newBuilder().setSocketAddress(AddressOuterClass.SocketAddress
                                    .newBuilder().setAddress(clusterDto.host).setPortValue(clusterDto.port).build()))
                    .build();
        }).collect(toList());
    }
}
