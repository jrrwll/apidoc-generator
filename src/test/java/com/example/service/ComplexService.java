package com.example.service;

import com.example.base.ApiPageSummary;
import com.example.base.ApiResult;
import com.example.param.ComplexCreateParam;
import com.example.param.ComplexListParam;
import com.example.result.ComplexModel;
import com.example.result.ComplexSummaryModel;

/**
 * @author Jerry Will
 * @version 2021-12-17
 */
public interface ComplexService {

    // get complex
    ApiResult<ComplexModel> get(String id);

    // list complex
    ApiResult<ApiPageSummary<ComplexModel, ComplexSummaryModel>> list(ComplexListParam param);

    /**
     * create complex
     *
     * @param param require param to create complex
     * @param file  attachment
     * @return complex id
     */
    ApiResult<String> create(ComplexCreateParam param, byte[] file);

    // update complex
    ApiResult<String> update(ComplexCreateParam param);

    // delete complex
    ApiResult<Void> delete(String id);
}
