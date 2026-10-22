package com.zhs.service.dto.auth;

import lombok.Data;

@Data
public class WxLoginCommand {

    private String code;
    private String nickname;
    private String avatar;
}
