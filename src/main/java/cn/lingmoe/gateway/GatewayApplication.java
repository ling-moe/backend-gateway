package cn.lingmoe.gateway;

import cn.lingmoe.redis.anno.Enable0moeMultiDbRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@EnableDiscoveryClient
@Enable0moeMultiDbRedis
@ComponentScan(excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = LoadBalancerClientConfiguration.ReactiveSupportConfiguration.class), @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = LoadBalancerClientConfiguration.class)})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
