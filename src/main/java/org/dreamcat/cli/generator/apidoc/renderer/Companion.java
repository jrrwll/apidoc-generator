package org.dreamcat.cli.generator.apidoc.renderer;

import java.io.File;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2022-01-19
 */
final class Companion {

    private Companion() {
    }

    static File getOutputPath(String outputPath, String suffix) {
        if (ObjectUtil.isNotBlank(outputPath)) {
            outputPath = outputPath.replaceAll("[.]+", "")
                    .replaceAll("[.]/", "")
                    .replaceAll("/[.]", "")
                    .replaceAll("/+", "/");
            if (outputPath.startsWith("/")) outputPath = outputPath.substring(1);
        }
        if (ObjectUtil.isBlank(outputPath)) outputPath = ".";
        if (outputPath.endsWith("." + suffix)) {
            return new File(outputPath);
        } else {
            return new File(outputPath, "apidoc." + suffix);
        }
    }
}
