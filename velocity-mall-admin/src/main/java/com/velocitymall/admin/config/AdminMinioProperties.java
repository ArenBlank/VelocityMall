package com.velocitymall.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class AdminMinioProperties {

    private String endpoint;

    private String publicEndpoint;

    private String accessKey;

    private String secretKey;

    private String bucketName;

    private long maxFileSize = 5 * 1024 * 1024L;
}
