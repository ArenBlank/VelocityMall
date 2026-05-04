package com.velocitymall.common.model.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic page response object.
 *
 * @param <T> record type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {

    private Long page;

    private Long size;

    private Long total;

    private Long pages;

    private List<T> records;
}
