package com.zhs.service.acq;

import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLException;

/**
 * 把唯一约束冲突转换成业务异常，避免把 SQL 返回给页面。
 */
public final class AcqDuplicate {

    private AcqDuplicate() {
    }

    public static boolean isDuplicate(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof DuplicateKeyException) {
                return true;
            }
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() == 1062) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
