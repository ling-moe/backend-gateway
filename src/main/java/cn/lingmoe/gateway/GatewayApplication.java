package cn.lingmoe.gateway;

import cn.lingmoe.redis.anno.Enable0moeMultiDbRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Enable0moeMultiDbRedis
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
