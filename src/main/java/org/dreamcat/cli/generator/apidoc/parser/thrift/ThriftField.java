package org.dreamcat.cli.generator.apidoc.parser.thrift;

import lombok.Data;

/**
 * @author Jerry Will
 * @version 2024-01-01
 */
@Data
public class ThriftField {

    Integer key;
    String name;
    ThriftTypeId typeId;
    ThriftType type;
    ThriftRequired required;
}
