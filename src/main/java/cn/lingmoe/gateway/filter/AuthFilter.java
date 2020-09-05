package cn.lingmoe.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.lingmoe.authclient.constants.AuthConstants;
import cn.lingmoe.authclient.provider.AuthClient;
import cn.lingmoe.common.Result.Ophilia;
import cn.lingmoe.core.exception.security.ForbiddenException;
import cn.lingmoe.core.exception.security.UnauthorizedException;
import cn.lingmoe.gateway.constants.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author yukdawn@gmail.com 2020/5/1 上午10:27
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @DubboReference
    private AuthClient authClient;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String authorization = Optional.ofNullable(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .map(s -> StrUtil.removePrefix(s, AuthConstants.TOKEN_TYPE)).orElse(null);
        String jwt;

        if (StrUtil.equals(request.getHeaders().getFirst(Constants.PUBLIC_ROUTER), Constants.HEADER_TRUE)) {
            // 如果是公开权限则直接放行到各个服务
            return chain.filter(exchange);
        } else if (StrUtil.isBlank(authorization)) {
            // 401错误，未登录
            throw new UnauthorizedException();
        } else if (StrUtil.equals(AuthConstants.NAME, request.getHeaders().getFirst(Constants.SERVICE_NAME))) {
            // 如果是auth服务，则仅进行鉴权，因为auth用的是redistokenstor，不需要jwt转换
            jwt = authClient.auth(authorization, request.getMethodValue(), request.getPath().value());
        } else {
            jwt = authClient.authAndConvert(authorization, request.getMethodValue(), request.getPath().value());
        }
        if (StrUtil.equals(jwt, AuthConstants.ErrorCode.NONE_LOGIN)) {
            // 401错误，未登录
            throw new UnauthorizedException();
        } else if (StrUtil.equals(jwt, AuthConstants.ErrorCode.NONE_PERMS)) {
            // 403错误，没有权限
            throw new ForbiddenException();
        }
        // 继续路由到目标服务
        ServerHttpRequest jwtRequest = request.mutate().header(HttpHeaders.AUTHORIZATION,
                AuthConstants.TOKEN_TYPE + jwt).build();
        ServerWebExchange jwtExchange = exchange.mutate().request(jwtRequest).build();
        return chain.filter(jwtExchange);
    }

    private Mono<Void> set401Response(ServerWebExchange exchange, String msg) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        originalResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
        originalResponse.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        byte[] response =
                JSONUtil.toJsonStr(new Ophilia(HttpStatus.UNAUTHORIZED.getReasonPhrase(), msg)).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = originalResponse.bufferFactory().wrap(response);
        return originalResponse.writeWith(Flux.just(buffer));
    }

    private Mono<Void> set403Response(ServerWebExchange exchange, String msg) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        originalResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
        originalResponse.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        byte[] response =
                JSONUtil.toJsonStr(new Ophilia(HttpStatus.FORBIDDEN.getReasonPhrase(), msg)).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = originalResponse.bufferFactory().wrap(response);
        return originalResponse.writeWith(Flux.just(buffer));
    }

    @Override
    public int getOrder() {
        return 30;
    }

}
