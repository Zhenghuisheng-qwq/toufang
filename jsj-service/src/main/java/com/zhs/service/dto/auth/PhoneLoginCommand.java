package com.zhs.service.dto.auth;

import lombok.Data;

@Data
public class PhoneLoginCommand {

    private String phone;
    private String code;
}
