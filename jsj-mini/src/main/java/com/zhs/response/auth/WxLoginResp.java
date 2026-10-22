package com.zhs.response.auth;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 登录响应
 */
@Data
public class WxLoginResp {

    /** 登录 token */
    private String token;

    /** 用户主键 ID（序列化为字符串，避免前端 Long 精度丢失） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;
}
