package org.dreamcat.cli.generator.apidoc.parser.thrift;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Jerry Will
 * @version 2024-01-01
 * @see <a href="https://thrift.apache.org/docs/types">Thrift Types</>
 **/
public enum ThriftTypeId {
    bool,
    @JsonProperty("byte")
    _byte,
    i16,
    i32,
    i64,
    @JsonProperty("double")
    _double,
    string,
    @JsonProperty("enum")
    _enum,
    list,
    set,
    map,
    struct,
}

