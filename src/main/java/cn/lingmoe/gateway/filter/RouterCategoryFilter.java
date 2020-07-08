package cn.lingmoe.gateway.filter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.lingmoe.authclient.constants.AuthConstants;
import cn.lingmoe.gateway.constants.Constants;
import cn.lingmoe.redis.multidb.RedisHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author yukdawn@gmail.com 2020/5/14 下午1:35
 */
@Component
public class RouterCategoryFilter implements GlobalFilter, Ordered {
    @Autowired
    private RedisHelper redisHelper;
    private AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getRawPath();
        String serviceName = request.getHeaders().getFirst(Constants.SERVICE_NAME);
        ServerHttpRequest.Builder builder = request.mutate();
        // 检查是否是公开权限，是则添加相应头文件
        Set<JSONObject> publicRouterSet = redisHelper.setMembers(AuthConstants.getPublicRouterKey(serviceName, request.getMethodValue()),
                JSONObject.class);
        String publicRouter = publicRouterSet.stream().map(obj -> obj.getByPath("path", String.class))
                .filter(pattern-> matcher.match(pattern, path))
                .findFirst().orElse(null);
        if (StrUtil.isNotBlank(publicRouter)){
            builder.header(Constants.PUBLIC_ROUTER, Constants.HEADER_TRUE);
        }
        // 检查是否是登录即用权限，是则添加相应头文件
        Set<JSONObject> loginRouterSet = redisHelper.setMembers(AuthConstants.getLoginRouterKey(serviceName, request.getMethodValue()),
                JSONObject.class);
        String loginRouter = loginRouterSet.stream().map(obj -> obj.getByPath("path", String.class))
                .filter(pattern-> matcher.match(pattern, path))
                .findFirst().orElse(null);
        if (StrUtil.isNotBlank(loginRouter)){
            builder.header(Constants.LOGIN_ROUTER, Constants.HEADER_TRUE);
        }
        exchange = exchange.mutate().request(builder.build()).build();
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
