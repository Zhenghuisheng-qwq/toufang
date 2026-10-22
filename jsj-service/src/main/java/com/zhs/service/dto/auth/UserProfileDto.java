package com.zhs.service.dto.auth;

import lombok.Data;

@Data
public class UserProfileDto {

    private Long userId;
    private String nickname;
    private String avatar;
}
