package org.dreamcat.cli.generator.apidoc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.cli.generator.apidoc.parser.ApiDocParser;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.function.OneAndArrayConsumer;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.reflect.ObjectRandomGenerator;
import org.dreamcat.common.util.RandomUtil;
import org.dreamcat.common.util.SystemUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class ApiDocGenerator {

    private final ApiDoc apiDoc;
    private final boolean verbose;

    private OneAndArrayConsumer<String> infoLogger = SystemUtil::println;
    private OneAndArrayConsumer<String> errorLogger = SystemUtil::printlnErr;

    public ApiDocGenerator(ApiDocParseConfig config) {
        this(new ApiDocParser(config), config.isVerbose());
    }

    public ApiDocGenerator(ApiDocParseConfig config, ClassLoader classLoader) {
        this(new ApiDocParser(config, classLoader), config.isVerbose());
    }

    public ApiDocGenerator(ApiDocParseConfig config, ObjectRandomGenerator randomGenerator) {
        this(new ApiDocParser(config, randomGenerator), config.isVerbose());
    }

    public ApiDocGenerator(ApiDocParseConfig config, ClassLoader classLoader,
            ObjectRandomGenerator randomGenerator) {
        this(new ApiDocParser(config, classLoader, randomGenerator), config.isVerbose());
    }

    private ApiDocGenerator(ApiDocParser apiDocParser, boolean verbose) {
        this.apiDoc = apiDocParser.parse();
        this.verbose = verbose;
    }

    public String generate(ApiDocRenderer renderer) throws IOException {
        if (verbose) {
            infoLogger.accept("renderer {}:\n{}", renderer.getClass().getName(),
                    JsonUtil.toJsonWithPretty(renderer));
        }
        return renderer.render(apiDoc);
    }

    public void generate(ApiDocRenderer renderer, File outputDir) throws IOException {
        String doc = generate(renderer);
        String suffix = renderer.getOutputFileSuffix();
        if (suffix == null || doc == null) {
            // log.debug("{} is unsupported to output to file, skip", renderer.getClass());
            return;
        }

        if (outputDir == null) {
            infoLogger.accept("only print doc since `outputDir` is unset");
            infoLogger.accept("********** Generated Doc **********");
            infoLogger.accept("\n" + doc); // print doc to console
            infoLogger.accept("***********************************");
            return;
        }

        outputDir = outputDir.getCanonicalFile();
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            errorLogger.accept(
                    "failed to mkdirs output dir: {}", outputDir);
            return;
        }

        File outputFile = new File(outputDir, "apidoc-" + RandomUtil.uuid32() + "." + suffix);
        try {
            infoLogger.accept("writing to {}", outputFile);
            FileUtil.write(outputFile, doc);
            infoLogger.accept("done");
        } catch (IOException e) {
            errorLogger.accept("error to write file {}, doc:\n {}", outputFile, doc);
        }
    }
}
