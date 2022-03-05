package com.example.base;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Getter
@Setter
public class ApiPage<T> extends PageParam {

    private List<T> list; // the list data per page
    private int totalCount; // the total count
    private int totalPage; // the total page
}
