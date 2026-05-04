package com.velocitymall.search.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search index rebuild result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebuildIndexVO {

    private Long indexedCount;

    private String message;
}
