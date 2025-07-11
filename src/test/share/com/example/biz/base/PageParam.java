package com.example.biz.base;

import com.example.base.BaseParam;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2021-12-17
 */
@Getter
@Setter
public class PageParam extends BaseParam {

    private int pageNo = 1; // page number, default is 1
    private int pageSize = 10; // page size, default is 10
}
