package com.zhs.service.acq.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LandingPageSaveCommand {
    private String title;
    private String headline;
    private String sellingText;
    private String buttonText;
    private String priceYuan;
    private Integer publishStatus;
    private Integer sortNo;
    private Long reportTemplateId;
    private Integer nameEnabled;
    private Integer genderEnabled;
    private Integer birthDateEnabled;
    private Integer birthTimeEnabled;
    private List<LandingAssetCommand> assets = new ArrayList<>();
}
