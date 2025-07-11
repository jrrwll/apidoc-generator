package com.example.biz.result;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2021-12-17
 */
@Getter
@Setter
public class CommonModel {

    ///! entity id
    private String id;
    /*
    who created the record
    */
    private Long createdBy;
    private Date createdAt; /* when the record was created at */
}
