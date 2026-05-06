package com.velocitymall.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base payload for asynchronous MQ messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseMessageDTO {

    private String traceId;

    private String businessId;
}
