package com.zhs.common.exception;

/**
 * 业务异常（jsj-common / jsj-service 层使用，避免与 Admin 侧 ServiceException 混用）
 */
public final class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = null;
    }

    public BusinessException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
