package com.zhs.service.acq;

/**
 * 投放业务状态码。与库表 COMMENT 一致，沿用整型状态，不另建枚举框架。
 */
public final class AcquisitionCodes {

    private AcquisitionCodes() {
    }

    public static final int PLATFORM_UNKNOWN = 0;
    public static final int PLATFORM_TENCENT = 1;
    public static final int PLATFORM_BAIDU = 2;
    public static final int PLATFORM_KUAISHOU = 3;

    public static final int TRADE_H5 = 1;
    public static final int TRADE_JSAPI = 2;

    public static final int PAY_UNPAID = 0;
    public static final int PAY_SUCCESS = 1;

    public static final int ENTRY_CONTACT_QR = 1;
    public static final int ENTRY_ACQUISITION_LINK = 2;

    public static final int ROUTE_GLOBAL = 1;
    public static final int ROUTE_LANDING = 2;
    public static final int ROUTE_ENABLED = 1;

    public static final int TASK_REPORT = 1;
    public static final int TASK_WECOM_CONTACT = 2;
    public static final int TASK_AD_CONVERSION = 3;

    public static final int TASK_PENDING = 0;
    public static final int TASK_RUNNING = 1;
    public static final int TASK_UNKNOWN = 4;

    public static final int DOMAIN_MERCHANT_ENABLED = 1;
    public static final int DOMAIN_MERCHANT_VERIFIED = 1;

    public static final int PAGE_DRAFT = 0;
    public static final int PAGE_PUBLISHED = 1;
    public static final int PAGE_OFFLINE = 2;

    public static final int TEMPLATE_DRAFT = 0;
    public static final int TEMPLATE_ENABLED = 1;
    public static final int TEMPLATE_DISABLED = 2;

    public static final int VERSION_DRAFT = 0;
    public static final int VERSION_PUBLISHED = 1;

    public static final int SWITCH_OFF = 0;
    public static final int SWITCH_ON = 1;
    public static final int ASSET_IMAGE = 1;
    public static final int RECORD_ENABLED = 1;
    public static final int RECORD_DISABLED = 0;
    public static final int CONTENT_TEXT = 1;
    public static final int EVENT_PAY = 1;

    public static final int[] POST_PAY_TASK_TYPES = {
            TASK_REPORT, TASK_WECOM_CONTACT, TASK_AD_CONVERSION
    };
}
