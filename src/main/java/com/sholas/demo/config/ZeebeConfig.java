package com.sholas.demo.config;

import io.camunda.zeebe.client.ZeebeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;

@Configuration
public class ZeebeConfig {

    @Value("${camunda.client.cloud.cluster-id}")
    private String clusterId;

    @Value("${camunda.client.auth.client-id}")
    private String clientId;

    @Value("${camunda.client.auth.client-secret}")
    private String clientSecret;

    @Value("${camunda.client.cloud.region}")
    private String region;

    @Bean
    public ZeebeClient zeebeClient() {
        return ZeebeClient.newCloudClientBuilder()
                .withClusterId(clusterId)
                .withClientId(clientId)
                .withClientSecret(clientSecret)
                .withRegion(region)
                .build();
    }
}

