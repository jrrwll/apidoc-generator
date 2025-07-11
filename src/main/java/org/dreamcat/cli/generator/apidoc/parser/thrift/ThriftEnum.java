package org.dreamcat.cli.generator.apidoc.parser.thrift;

import lombok.Data;

import java.util.List;

/**
 * @author Jerry Will
 * @version 2024-01-01
 */
@Data
public class ThriftEnum {

    String name;
    String doc;
    List<Member> members;

    @Data
    public static class Member {

        String name;
        Integer value; // 0, 1, 2
    }
}
