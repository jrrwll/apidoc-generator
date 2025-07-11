package org.dreamcat.cli.generator.apidoc.parser.thrift;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jerry Will
 * @version 2024-01-01
 */
@Data
public class ThriftDef {

    String name;
    // example: java -> x.y.z.mypkg
    Map<String, String> namespaces;
    Set<String> includes;
    List<ThriftEnum> enums;
    // List<ThriftTypedef> typedefs;
    List<ThriftStruct> structs;
    // List<ThriftConstant> constants;
    List<ThriftService> services;

}

