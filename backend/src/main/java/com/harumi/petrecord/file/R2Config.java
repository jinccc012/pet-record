package com.harumi.petrecord.file;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(R2Properties.class)
public class R2Config {

    private StaticCredentialsProvider credentials(R2Properties p) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(p.getAccessKeyId(), p.getSecretAccessKey()));
    }

    private S3Configuration pathStyle() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    @Bean
    public S3Client s3Client(R2Properties p) {
        return S3Client.builder()
                .endpointOverride(URI.create(p.getEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(credentials(p))
                .httpClient(UrlConnectionHttpClient.create())
                .serviceConfiguration(pathStyle())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(R2Properties p) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(p.getEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(credentials(p))
                .serviceConfiguration(pathStyle())
                .build();
    }
}
