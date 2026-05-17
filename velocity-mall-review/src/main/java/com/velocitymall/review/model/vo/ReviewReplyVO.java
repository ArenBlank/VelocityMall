package com.velocitymall.review.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product review reply view object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String content;

    private Boolean mine;

    private LocalDateTime createTime;
}
