package com.velocitymall.admin.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginVO {

    private String token;

    private Long adminId;

    private String username;

    private String realName;
}
