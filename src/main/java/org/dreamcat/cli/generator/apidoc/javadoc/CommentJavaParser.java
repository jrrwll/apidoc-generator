package org.dreamcat.cli.generator.apidoc.javadoc;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
public class CommentJavaParser {

    private CommentJavaParser() {
    }

    public static CommentClassDef parseOne(String javaFilePath, List<String> srcDirs, String className) {
        return parseOne(new File(javaFilePath), srcDirs, className);
    }

    public static CommentClassDef parseOne(File javaFilePath, List<String> srcDirs, String className) {
        List<CommentClassDef> defs = parse(javaFilePath, srcDirs);
        for (CommentClassDef def : defs) {
            if (def.getType().equals(className.replace('$', '.'))) {
                return def;
            }
        }
        throw new IllegalArgumentException("no class " + className +
                " defined in file " + javaFilePath.getAbsolutePath());
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

        List<CommentClassDef> defs = new ArrayList<>();
        for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
            recurseParse(type, defs);
        }
        return defs;
    }

    private static void recurseParse(TypeDeclaration<?> type, List<CommentClassDef> defs) {
        defs.add(new CommentClassDef(type));
        NodeList<BodyDeclaration<?>> members = type.getMembers();
        if (ObjectUtil.isEmpty(members)) return;

        for (BodyDeclaration<?> member : members) {
            if (member.isClassOrInterfaceDeclaration()) {
                TypeDeclaration<?> subType = member.asClassOrInterfaceDeclaration();
                recurseParse(subType, defs);
            }
        }
    }
}
