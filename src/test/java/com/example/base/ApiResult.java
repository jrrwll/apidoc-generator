package com.example.base;

import lombok.Data;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class ApiResult<T> {

    private String code = "200"; // response code
    private String msg = "success"; // error message
    private T data; // real data
}
