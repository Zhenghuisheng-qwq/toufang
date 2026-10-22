package com.zhs.service.dto.auth;

import lombok.Data;

@Data
public class LoginResult {

    private String token;
    private Long userId;
    private String nickname;
    private String avatar;
}
