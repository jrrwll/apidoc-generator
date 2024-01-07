package org.dreamcat.cli.generator.apidoc.parser.thrift;

import java.util.List;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2024-01-01
 */
@Data
public class ThriftService {

    String name;
    String doc;
    List<ThriftFunction> functions;
}