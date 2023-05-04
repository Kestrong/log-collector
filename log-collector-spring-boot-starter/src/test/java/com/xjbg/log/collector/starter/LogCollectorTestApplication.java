package com.xjbg.log.collector.starter;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import javax.sql.DataSource;

/**
 * @author kesc
 * @since 2020-04-03 8:48
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableDiscoveryClient
@EnableCircuitBreaker
@EnableFeignClients(basePackages = "com.xjbg.log.collector.starter.example.feign")
@SpringBootApplication
public class LogCollectorTestApplication {

    //custom datasource for database log collector
    @Bean(name = "logCollectorDataSource")
    public DataSource logCollectorDataSource(DataSourceProperties properties) {
        HikariDataSource hikariDataSource1 = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        hikariDataSource1.setPoolName("logCollectorDataSource");
        return hikariDataSource1;
    }

    public static void main(String[] args) {
        SpringApplication.run(LogCollectorTestApplication.class, args);
    }

}
