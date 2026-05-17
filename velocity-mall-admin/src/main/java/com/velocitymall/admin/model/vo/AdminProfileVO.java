package com.velocitymall.admin.model.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileVO {

    private String adminId;

    private String username;

    private String realName;

    private List<String> roles;

    private List<String> permissions;
}
