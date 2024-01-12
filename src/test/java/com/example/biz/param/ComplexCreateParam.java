package com.example.biz.param;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2021-12-17
 */
@Data
public class ComplexCreateParam {

    private BigInteger a; // some of complex
    // some of complex
    private BigDecimal b;
    /** props for extra settings */
    private Map<String, String> props;
}
