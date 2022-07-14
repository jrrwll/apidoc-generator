package com.example.controller;

import com.example.base.ApiPageSummary;
import com.example.base.ApiResult;
import com.example.param.ComplexCreateParam;
import com.example.param.ComplexListParam;
import com.example.result.ComplexModel;
import com.example.result.ComplexSummaryModel;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Jerry Will
 * @version 2021-12-17
 */
@RestController
@RequestMapping(path = "/api/v1", method = RequestMethod.POST)
public class ComplexController {

    // get complex
    @RequestMapping(path = {"/get", "/info"}, method = RequestMethod.GET)
    public ApiResult<ComplexModel> get(@RequestParam("id") String id) {
        return null;
    }

    @RequestMapping(path = {"/get/withVersion"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult<ComplexModel> getWithVersion(
            @RequestParam(value = "id", required = true) String id,
            @RequestParam(value = "version", required = false) Integer version) {
        return null;
    }

    // list complex
    @RequestMapping(path = "list", method = RequestMethod.GET)
    public ApiResult<ApiPageSummary<ComplexModel, ComplexSummaryModel>> list(
            @RequestBody ComplexListParam param) {
        return null;
    }

    // create complex
    @RequestMapping(path = "/create")
    public ApiResult<String> create(
            @RequestBody ComplexCreateParam param,
            @RequestPart(name = "file", required = false) MultipartFile file) {
        return null;
    }

    /**
     * update complex
     *
     * @param param create param
     * @return updated id
     */
    @RequestMapping(path = {"/update", "edit"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public ApiResult<String> update(
            @RequestBody ComplexCreateParam param) {
        return null;
    }

    // delete complex
    @RequestMapping(path = "/delete/{id}", method = {RequestMethod.DELETE, RequestMethod.POST, RequestMethod.PUT})
    public ApiResult<Void> delete(@PathVariable("id") String id) {
        return null;
    }
}
