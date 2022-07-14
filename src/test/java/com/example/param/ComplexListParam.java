package com.example.param;

import com.example.base.PageParam;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2021-12-17
 */
@Getter
@Setter
public class ComplexListParam extends PageParam {

    @NotBlank
    private String token; // the token to sign
    @NotEmpty
    private Set<Integer> userIds; // users who admire the number
    /**
     * ext info
     */
    private Ext ext;

    public static class Ext {

        // unix timestamp
        private long timestamp;
        @NotNull
        private Ext2 ext2; /* ext2 */
    }

    public static class Ext2 {

        /* version */
        @NotNull
        private Integer version;
    }
}
