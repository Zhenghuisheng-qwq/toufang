package com.zhs.service.acq.report;

import java.time.LocalDate;
import java.time.LocalTime;

/** Immutable rendering input. A missing birth time remains null. */
public record ReportInput(String name, Integer gender, LocalDate birthDate, LocalTime birthTime) {
    @Override
    public String toString() {
        return "ReportInput[redacted]";
    }
}
