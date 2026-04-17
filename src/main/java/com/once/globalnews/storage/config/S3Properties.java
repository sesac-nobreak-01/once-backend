package com.once.globalnews.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "storage.s3")
public class S3Properties {

    private String bucket;
    private String region;
    private int presignTtlSeconds = 600;
    private long maxObjectSizeBytes = 33_554_432L;
    private Set<String> allowedImageMimeTypes = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif"
    );
}
