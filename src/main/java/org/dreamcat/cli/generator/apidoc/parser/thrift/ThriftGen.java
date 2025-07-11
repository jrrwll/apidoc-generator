package org.dreamcat.cli.generator.apidoc.parser.thrift;

import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.io.ShellUtil;
import org.dreamcat.common.json.JsonUtil;

import java.io.File;
import java.nio.file.Files;

/**
 * @author Jerry Will
 * @version 2024-01-01
 */
@Slf4j
public class ThriftGen {

    // mark sure thrift in your PATH private
    String thriftPath = "/opt/homebrew/bin/thrift";

    public ThriftDef parse(String thriftFile) throws Exception {
        File dir = Files.createTempDirectory("thrift-gen").toFile();
        log.info("dir: {}", dir);
        try {
            ShellUtil.exec(true, thriftPath, "-gen", "json",
                    "-out", dir.getAbsolutePath(), thriftFile);
            File outputFile = new File(dir, FileUtil.prefix(thriftFile) + ".json");
            return JsonUtil.fromJson(outputFile, ThriftDef.class);
        } finally {
            FileUtil.deleteRecursively(dir);
        }
    }
}
