package org.dreamcat.cli.generator.apidoc.javadoc;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
public class CommentJavaParser {

    public static CommentClassDef parseOne(String javaFilePath, List<String> srcDirs, String className) {
        return parseOne(new File(javaFilePath), srcDirs, className);
    }

    public static CommentClassDef parseOne(File javaFilePath, List<String> srcDirs, String className) {
        CommentClassDef classDef = parse(javaFilePath, srcDirs).stream()
                .filter(it -> it.getType().equals(className))
                .findAny().orElse(null);
        if (classDef == null) {
            throw new IllegalArgumentException("no class " + className +
                    " defined in file " + javaFilePath.getAbsolutePath());
        }
        return classDef;
    }

    public static List<CommentClassDef> parse(String javaFilePath, List<String> srcDirs) {
        return parse(new File(javaFilePath), srcDirs);
    }

    public static List<CommentClassDef> parse(File javaFilePath, List<String> srcDirs) {
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(JavaParserUtil.symbolResolver(srcDirs));
        JavaParser parser = new JavaParser(config);

        ParseResult<CompilationUnit> parseResult;
        try {
            parseResult = parser.parse(javaFilePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        CompilationUnit compilationUnit = parseResult.getResult().orElse(null);
        if (compilationUnit == null) {
            String msg = parseResult.getProblems().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"));
            throw new RuntimeException(msg);
        }

        return compilationUnit.getTypes().stream().map(CommentClassDef::new)
                .collect(Collectors.toList());
    }

    private static String getSimpleName(String javaFilePath) {
        int ps = javaFilePath.lastIndexOf(File.pathSeparator);
        int ds = javaFilePath.lastIndexOf('.');
        return javaFilePath.substring(ps + 1, ds);
    }
}
