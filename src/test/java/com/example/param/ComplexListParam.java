package com.example.param;

import com.example.base.PageParam;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2021-12-17
 */
@Getter
@Setter
public class ComplexListParam extends PageParam {

    private String token; // the token to sign
    private Set<Integer> userIds; // users who admire the number
}
