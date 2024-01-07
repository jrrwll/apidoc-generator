package org.dreamcat.cli.generator.apidoc.parser.thrift;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2024-01-01
 */
@Data
public class ThriftType {

    ThriftTypeId typeId;
    ThriftTypeId elemTypeId;
    ThriftType elemType;
    @JsonProperty("class")
    String _class;
}
