package com.zhs.response.auth;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 手机号登录响应（与 WxLoginResp 保持字段一致，前端统一处理）
 */
@Data
public class PhoneLoginResp {

    private String token;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String nickname;

    private String avatar;
}
