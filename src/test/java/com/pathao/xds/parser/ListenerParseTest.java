package com.pathao.xds.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.BoolValue;
import envoy.api.v2.Lds;
import envoy.api.v2.Rds;
import envoy.api.v2.core.AddressOuterClass;
import envoy.api.v2.route.RouteOuterClass;
import envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManagerOuterClass;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;

public class ListenerParseTest {
    @Test
    public void deserialize() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(getTestStr());
        List<Lds.Listener> listeners = ListenerParser.buildListener(jsonNode);
        assertTrue(listeners.size() == 1);
        assertTrue(listeners.get(0).equals(expectedListener()));
    }

    public Lds.Listener expectedListener() {
        envoy.api.v2.listener.Listener.FilterChain filterChain = envoy.api.v2.listener.Listener.FilterChain.newBuilder()
                .addFilters(envoy.api.v2.listener.Listener.Filter.newBuilder()
                        .setName("envoy.http_connection_manager")
                        .setConfig(ListenerParser.messageAsStruct(HttpConnectionManagerOuterClass
                                .HttpConnectionManager.newBuilder()
                                .setStatPrefix("ingress_http")
                                .setCodecType(HttpConnectionManagerOuterClass.HttpConnectionManager.CodecType.AUTO)
                                .setGenerateRequestId(BoolValue.of(true))
                                .setTracing(HttpConnectionManagerOuterClass.HttpConnectionManager.Tracing.newBuilder().setOperationName(HttpConnectionManagerOuterClass.HttpConnectionManager.Tracing.OperationName.EGRESS).build())
                                .setRouteConfig(Rds.RouteConfiguration.newBuilder()
                                        .setName("local_route")
                                        .addVirtualHosts(RouteOuterClass.VirtualHost.newBuilder()
                                                .setName("app1_http")
                                                .addAllDomains(asList("*"))
                                                .addRoutes(RouteOuterClass.Route.newBuilder()
                                                        .setMatch(RouteOuterClass.RouteMatch.newBuilder().setPrefix("/").build())
                                                        .setRoute(RouteOuterClass.RouteAction.newBuilder().setCluster("local_service").build())
                                                        .build())
                                                .build())
                                        .build())
                                .addHttpFilters(HttpConnectionManagerOuterClass.HttpFilter.newBuilder().setName("envoy.router").build())
                                .build()))
                        .build())
                .build();

        return Lds.Listener.newBuilder()
                .setName("http_listener")
                .setAddress(AddressOuterClass.Address.newBuilder().setSocketAddress(AddressOuterClass
                        .SocketAddress.newBuilder().setAddress("0.0.0.0").setPortValue(80).build()).build())
                .addFilterChains(filterChain)
                .build();
    }


    public String getTestStr() {
        return "[\n" +
                "      {\n" +
                "        \"name\": \"http_listener\",\n" +
                "        \"address\": {\n" +
                "          \"socket_address\": {\n" +
                "            \"address\": \"0.0.0.0\",\n" +
                "            \"port_value\": 80\n" +
                "          }\n" +
                "        },\n" +
                "        \"filter_chains\": [\n" +
                "          {\n" +
                "            \"filters\": [\n" +
                "              {\n" +
                "                \"name\": \"envoy.http_connection_manager\",\n" +
                "                \"config\": {\n" +
                "                  \"stat_prefix\": \"ingress_http\",\n" +
                "                  \"codec_type\": \"AUTO\",\n" +
                "                  \"generate_request_id\": true,\n" +
                "                  \"tracing\": {\n" +
                "                    \"operation_name\": \"egress\"\n" +
                "                  },\n" +
                "                  \"route_config\": {\n" +
                "                    \"name\": \"local_route\",\n" +
                "                    \"virtual_hosts\": [\n" +
                "                      {\n" +
                "                        \"name\": \"app1_http\",\n" +
                "                        \"domains\": [\n" +
                "                          \"*\"\n" +
                "                        ],\n" +
                "                        \"routes\": [\n" +
                "                          {\n" +
                "                            \"match\": {\n" +
                "                              \"prefix\": \"/\"\n" +
                "                            },\n" +
                "                            \"route\": {\n" +
                "                              \"cluster\": \"local_service\"\n" +
                "                            }\n" +
                "                          }\n" +
                "                        ]\n" +
                "                      }\n" +
                "                    ]\n" +
                "                  },\n" +
                "                  \"http_filters\": [\n" +
                "                    {\n" +
                "                      \"name\": \"envoy.router\"\n" +
                "                    }\n" +
                "                  ]\n" +
                "                }\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]";
    }
}
