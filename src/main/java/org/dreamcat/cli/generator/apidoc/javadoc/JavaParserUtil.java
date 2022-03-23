package org.dreamcat.cli.generator.apidoc.javadoc;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
class JavaParserUtil {

    private JavaParserUtil() {
    }

    public static SymbolResolver symbolResolver(List<String> srcDirs) {
        TypeSolver typeSolver = getTypeSolver(srcDirs);
        return new JavaSymbolSolver(typeSolver);
    }

    public static TypeSolver getTypeSolver(List<String> srcDirs) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        for (String srcDir : srcDirs) {
            typeSolver.add(new JavaParserTypeSolver(new File(srcDir)));
        }
        return typeSolver;
    }

    public static String getJavadocComment(Node node) {
        String comment = getComment(node);
        return Arrays.stream(comment.split("\n"))
                .filter(ObjectUtil::isNotBlank)
                .map(it -> StringUtil.trimLeft(it, " *\r\t"))
                .filter(it -> !it.startsWith("@"))
                .filter(ObjectUtil::isNotBlank)
                .collect(Collectors.joining("\n"));
    }

    public static String getComment(Node node) {
        return node.getComment().map(Comment::getContent).orElse("");
    }
}
