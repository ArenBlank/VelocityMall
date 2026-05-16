package com.velocitymall.product.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fallback cover pool for SKUs without a manually uploaded cover image.
 */
@Data
@Component
@ConfigurationProperties(prefix = "product.cover-fallback")
public class ProductCoverFallbackProperties {

    private List<String> urls = new ArrayList<>();

    public String pick(Long skuId) {
        List<String> candidates = urls == null
                ? List.of()
                : urls.stream().filter(StringUtils::hasText).toList();
        if (candidates.isEmpty()) {
            return null;
        }
        long seed = skuId == null ? 0L : skuId;
        return candidates.get(Math.floorMod(seed, candidates.size()));
    }
}
