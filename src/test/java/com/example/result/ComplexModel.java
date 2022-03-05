package com.example.result;

import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2021-12-17
 */
@Getter
@Setter
public class ComplexModel extends CommonModel {

    private int a; // a or a + bi
    // b of a + bi
    private Integer b;
    /* salt to sign */
    private byte[] salt;
    /**
     * the people who admire the number
     */
    private List<User> admired;

    @Data
    public static class User {

        private long id; // user id
        private String name; // username
        private Gender gender; // gender to identify
    }

    public enum Gender {
        unknown,
        female,
        male;
    }
}
