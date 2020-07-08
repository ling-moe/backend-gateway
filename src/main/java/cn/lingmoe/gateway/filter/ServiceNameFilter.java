package cn.lingmoe.gateway.filter;


import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.addOriginalRequestUrl;

import java.util.Arrays;
import java.util.stream.Collectors;

import cn.lingmoe.gateway.constants.Constants;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author yukdawn@gmail.com 2020/5/14 下午2:26
 */
@Component
public class ServiceNameFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        addOriginalRequestUrl(exchange, request.getURI());
        String path = request.getURI().getRawPath();
        String serviceName = Arrays.stream(StringUtils.tokenizeToStringArray(path, "/"))
                .limit(1).collect(Collectors.joining("/"));
        String newPath = "/"
                + Arrays.stream(StringUtils.tokenizeToStringArray(path, "/"))
                .skip(1).collect(Collectors.joining("/"));
        newPath += (newPath.length() > 1 && path.endsWith("/") ? "/" : "");
        ServerHttpRequest newRequest = request.mutate().path(newPath)
                .header(Constants.SERVICE_NAME, serviceName).build();

        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR,
                newRequest.getURI());

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    @Override
    public int getOrder() {
        return 10;
    }


}
