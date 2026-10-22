package com.zhs.service.acq.dto;

import lombok.Data;

@Data
public class LandingPageQuery {
    private String title;
    private Integer publishStatus;
    private Long pageNum;
    private Long pageSize;
}
