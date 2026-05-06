package com.velocitymall.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;
}
