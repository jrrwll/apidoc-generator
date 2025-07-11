package org.dreamcat.cli.generator.apidoc.parser.thrift;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author Jerry Will
 * @version 2024-01-01
 */
@Data
public class ThriftStruct {

    String name;
    String doc;
    @JsonProperty("isException")
    boolean exception;
    @JsonProperty("isUnion")
    boolean union;
    List<ThriftField> fields;
}
