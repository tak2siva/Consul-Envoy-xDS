package com.pathao.xds.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.BoolValue;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import envoy.api.v2.Lds;
import envoy.api.v2.Rds;
import envoy.api.v2.core.AddressOuterClass;
import envoy.api.v2.route.RouteOuterClass;
import envoy.config.filter.network.http_connection_manager.v2.HttpConnectionManagerOuterClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListenerParser {
    public static List<Lds.Listener> buildListeners(JsonNode jsonNode) {
        List<Lds.Listener> listeners = new ArrayList<>();
        jsonNode.forEach(jListener -> {
            listeners.add(buildListener(jListener));
        });
        return listeners;
    }

    public static Lds.Listener buildListener(JsonNode jListener) {
        return Lds.Listener.newBuilder()
                .setName(jListener.get("name").asText())
                .setAddress(buildAddress(jListener.get("address")))
                .addAllFilterChains(buildFilerChains(jListener.get("filter_chains")))
                .build();
    }

    private static AddressOuterClass.Address buildAddress(JsonNode jListener) {
        return AddressOuterClass.Address.newBuilder()
                .setSocketAddress(AddressOuterClass.SocketAddress.newBuilder()
                        .setAddress(jListener.get("socket_address").get("address").asText())
                        .setPortValue(jListener.get("socket_address").get("port_value").asInt())
                        .build())
                .build();
    }

    private static List<envoy.api.v2.listener.Listener.FilterChain> buildFilerChains(JsonNode jFilterChains) {
        List<envoy.api.v2.listener.Listener.FilterChain> filterChains = new ArrayList<>();
        jFilterChains.forEach(jFilterChain -> {
            filterChains.add(
            envoy.api.v2.listener.Listener.FilterChain.newBuilder()
                    .addAllFilters(buildFilters(jFilterChain.get("filters")))
                    .build()
            );
        });
        return filterChains;
    }

    private static List<envoy.api.v2.listener.Listener.Filter> buildFilters(JsonNode jFilters) {
        List<envoy.api.v2.listener.Listener.Filter> filters = new ArrayList<>();
        jFilters.forEach(jFilter -> {
            filters.add(envoy.api.v2.listener.Listener.Filter.newBuilder()
                    .setName(jFilter.get("name").asText())
                    .setConfig(buildFilterConfig(jFilter.get("config")))
                    .build()
            );
        });
        return filters;
    }

    private static Struct buildFilterConfig(JsonNode jFilterConfig) {
        HttpConnectionManagerOuterClass.HttpConnectionManager filterConfig = HttpConnectionManagerOuterClass.HttpConnectionManager.newBuilder()
                .setStatPrefix(jFilterConfig.get("stat_prefix").asText())
                .setCodecType(HttpConnectionManagerOuterClass.HttpConnectionManager
                        .CodecType.valueOf(jFilterConfig.get("codec_type").asText()))
                .setGenerateRequestId(BoolValue.of(jFilterConfig.get("generate_request_id").asBoolean()))
                .setTracing(buildTracing(jFilterConfig.get("tracing")))
                .setRouteConfig(buildRouteConfig(jFilterConfig.get("route_config")))
                .addAllHttpFilters(buildHttpFilters(jFilterConfig.get("http_filters")))
                .build();
        return messageAsStruct(filterConfig);
    }

    private static List<HttpConnectionManagerOuterClass.HttpFilter> buildHttpFilters(JsonNode jHttpFilters) {
        List<HttpConnectionManagerOuterClass.HttpFilter> filters = new ArrayList<>();
        jHttpFilters.forEach(jHttpFilter -> {
            filters.add(HttpConnectionManagerOuterClass.HttpFilter.newBuilder()
                    .setName(jHttpFilter.get("name").asText())
                    .build()
            );
        });
        return filters;
    }

    private static HttpConnectionManagerOuterClass.HttpConnectionManager.Tracing buildTracing(JsonNode jTracing) {
        if (jTracing == null) {
            return HttpConnectionManagerOuterClass.HttpConnectionManager.Tracing.getDefaultInstance();
        }
        return HttpConnectionManagerOuterClass.HttpConnectionManager.Tracing
                .newBuilder().setOperationName(HttpConnectionManagerOuterClass.HttpConnectionManager.Tracing
                        .OperationName.valueOf(jTracing.get("operation_name").asText().toUpperCase()))
                .build();
    }

    private static Rds.RouteConfiguration buildRouteConfig(JsonNode jRouteConfig) {
        return Rds.RouteConfiguration.newBuilder()
                .setName(jRouteConfig.get("name").asText())
                .addAllVirtualHosts(buildVirtualHosts(jRouteConfig.get("virtual_hosts")))
                .build();
    }

    public static List<RouteOuterClass.VirtualHost> buildVirtualHosts(JsonNode jVirtualHosts) {
        List<RouteOuterClass.VirtualHost> virtualHosts = new ArrayList<>();
        jVirtualHosts.forEach(jvh -> {
            virtualHosts.add(RouteOuterClass.VirtualHost.newBuilder()
                    .setName(jvh.get("name").asText())
                    .addAllDomains(buildDomains(jvh.get("domains")))
                    .addAllRoutes(buildRoutes(jvh.get("routes")))
                    .build()
            );
        });
        return virtualHosts;
    }

    public static List<String> buildDomains(JsonNode jDomains) {
        List<String> res = new ArrayList<>();
        jDomains.forEach(x -> res.add(x.asText()));
        return res;
    }

    private static List<RouteOuterClass.Route> buildRoutes(JsonNode jRoutes) {
        List<RouteOuterClass.Route> routes = new ArrayList<>();
        jRoutes.forEach(jRoute -> {
            routes.add(RouteOuterClass.Route.newBuilder()
                    .setMatch(buildMatch(jRoute.get("match")))
                    .setRoute(buildRoute(jRoute.get("route")))
                    .build()
            );
        });
        return routes;
    }

    private static RouteOuterClass.RouteAction buildRoute(JsonNode jRoute) {
        Optional<String> prefix_rewrite = Optional.ofNullable(jRoute.get("prefix_rewrite")).map(JsonNode::asText);
        RouteOuterClass.RouteAction.Builder route = RouteOuterClass.RouteAction.newBuilder()
                .setCluster(jRoute.get("cluster").asText());

        prefix_rewrite.ifPresent(route::setPrefixRewrite);
        return route.build();
    }

    private static RouteOuterClass.RouteMatch buildMatch(JsonNode jMatch) {
        return RouteOuterClass.RouteMatch.newBuilder().setPrefix(jMatch.get("prefix").asText()).build();
    }

    public static Struct messageAsStruct(MessageOrBuilder message) {
        try {
            String json = JsonFormat.printer()
                    .preservingProtoFieldNames()
                    .print(message);

            Struct.Builder structBuilder = Struct.newBuilder();

            JsonFormat.parser().merge(json, structBuilder);

            return structBuilder.build();
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("Failed to convert protobuf message to struct", e);
        }
    }
}
