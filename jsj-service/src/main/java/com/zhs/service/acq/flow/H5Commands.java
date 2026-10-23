package com.zhs.service.acq.flow;

import java.time.LocalDate;
import java.time.LocalTime;

/** Public commands deliberately cannot bind money, merchant, route or payment status. */
public final class H5Commands {

    private H5Commands() {
    }

    public record Visit(Long pageId, String requestKey, Integer platform, String clickId) {
    }

    public record Lead(Long visitId, String requestKey, String name, Integer gender, LocalDate birthDate, LocalTime birthTime) {
        @Override
        public String toString() {
            return "Lead[redacted]";
        }
    }

    public record Order(Long leadId, String requestKey, Integer tradeType) {
    }
}
