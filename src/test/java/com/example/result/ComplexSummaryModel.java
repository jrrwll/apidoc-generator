package com.example.result;

import lombok.Data;

/**
 * @author Jerry Will
 * @version 2021-12-17
 */
@Data
public class ComplexSummaryModel {

    private long todayCount; // count of today
    private long last7dayCount; // count of last 7 day
    private Integer[] bits; // magic bits
}
