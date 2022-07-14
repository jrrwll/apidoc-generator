package com.example.service;

import com.example.base.ApiContext;
import com.example.base.ApiPageSummary;
import com.example.base.ApiResult;
import com.example.param.ComplexCreateParam;
import com.example.param.ComplexListParam;
import com.example.result.ComplexModel;
import com.example.result.ComplexSummaryModel;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author Jerry Will
 * @version 2021-12-17
 */
public interface ComplexService {

    // get complex
    ApiResult<ComplexModel> get(
            @NotBlank(message = "id is required") String id);

    /**
     * get complex with view
     *
     * @param id      complex id
     * @param version complex version
     * @return a complex model
     */
    ApiResult<ComplexModel> getWithVersion(
            @NotBlank(message = "id is required") String id,
            Integer version);

    // list complex
    ApiResult<ApiPageSummary<ComplexModel, ComplexSummaryModel>> list(
            @NotNull(message = "param is required") ComplexListParam param);

    /**
     * create complex
     *
     * @param param require param to create complex
     * @param file  attachment
     * @return complex id
     */
    ApiResult<String> create(
            @NotNull(message = "param is required") ComplexCreateParam param,
            @NotNull(message = "file is required") byte[] file);

    // update complex
    ApiResult<String> update(
            @NotNull(message = "param is required") ComplexCreateParam param);

    // delete complex
    ApiResult<Void> delete(
            @NotBlank(message = "id is required") String id,
            @NotNull(message = "context is required") ApiContext context);
}
