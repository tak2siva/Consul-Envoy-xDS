package com.pathao.xds.dao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitz.consul.KeyValueClient;
import com.pathao.xds.dto.ClusterDto;
import com.pathao.xds.parser.ListenerParser;
import envoy.api.v2.Lds;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ListenerDao {
    private KeyValueClient keyValueClient;

    public ListenerDao(KeyValueClient keyValueClient) {
        this.keyValueClient = keyValueClient;
    }

    public List<Lds.Listener> getListeners() {
        ObjectMapper mapper = new ObjectMapper();

        return keyValueClient.getKeys("listeners").stream().map(key -> {
            String name = key.replace("listeners/", "");
            Optional<String> valueAsString = keyValueClient.getValueAsString(key);
            Lds.Listener listener = null;


            if (valueAsString.isPresent()) {
                try {
                    JsonNode jsonNode = mapper.readTree(valueAsString.get());
                    listener = ListenerParser.buildListener(jsonNode);
                } catch (IOException e) {
                    System.out.println("Error on parsing Listener");
                    e.printStackTrace();
                }
            }

            return listener;
        }).collect(toList());
    }
}
