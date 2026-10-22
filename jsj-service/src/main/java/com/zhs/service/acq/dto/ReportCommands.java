package com.zhs.service.acq.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import com.zhs.service.acq.report.ReportInput;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ReportCommands {
    private ReportCommands() {
    }

    @Data
    public static class Preview {
        private String contentText;
        private ReportInput input;
    }

    public record PreviewResult(String contentText, String notice) {
        @Override
        public String toString() {
            return "PreviewResult[redacted]";
        }
    }

    @Data
    public static class Query {
        private String templateName;
        private Integer status;
        private Long pageNum;
        private Long pageSize;
    }

    @Data
    public static class Save {
        private String templateName;
        private String remark;
    }

    @Data
    public static class Draft {
        private String changeNote;
        private String contentText;
    }

    @Data
    public static class Row {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String templateName;
        private Integer status;
        private String remark;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long currentVersionId;
    }

    @Data
    public static class Version {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long templateId;
        private Integer versionNo;
        private Integer status;
        private String changeNote;
        private Integer contentType;
        private String contentText;
        private LocalDateTime publishedTime;
        private boolean current;
        private String previewNote = "以下为开发示例正文，不是正式算法输出。";
    }

    @Data
    public static class Detail {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private String templateName;
        private Integer status;
        private String remark;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long currentVersionId;
        private List<Version> versions = new ArrayList<>();
    }
}
