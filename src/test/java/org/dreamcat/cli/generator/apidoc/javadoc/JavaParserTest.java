package org.dreamcat.cli.generator.apidoc.javadoc;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.dreamcat.common.util.ObjectUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2024-01-06
 */
class JavaParserTest {

    String srcDir = new File("src/test/share").getAbsolutePath();

    @Test
    void test() throws Exception {
        List<String> srcDirs = Collections.singletonList(srcDir);
        File javaFilePath = new File(srcDir + "/com/example/biz/base/ApiResult.java");
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        for (String srcDir : srcDirs) {
            typeSolver.add(new JavaParserTypeSolver(new File(srcDir).getAbsoluteFile()));
        }
        // typeSolver.add(new ClassLoaderTypeSolver(Thread.currentThread().getContextClassLoader()));

        JavaSymbolSolver javaSymbolSolver = new JavaSymbolSolver(typeSolver);
        ParserConfiguration config = new ParserConfiguration();
        config.setSymbolResolver(javaSymbolSolver);
        JavaParser parser = new JavaParser(config);

        ParseResult<CompilationUnit> parseResult = parser.parse(javaFilePath);

        CompilationUnit compilationUnit = parseResult.getResult()
                .orElseThrow(RuntimeException::new);
        for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
            NodeList<BodyDeclaration<?>> members = type.getMembers();
            if (ObjectUtil.isEmpty(members)) continue;
            for (BodyDeclaration<?> member : members) {
                if (member.isClassOrInterfaceDeclaration()) {
                    TypeDeclaration<?> subType = member.asClassOrInterfaceDeclaration();
                    System.out.println("member: " + member + " = " + subType);
                } else if (member.isFieldDeclaration()) {
                    FieldDeclaration fieldDeclaration = (FieldDeclaration) member;
                    ResolvedFieldDeclaration resolved = fieldDeclaration.resolve();
                    String fieldType = null;
                    ResolvedType resolvedType = resolved.getType();
                    if (resolvedType.isReferenceType()) {
                        fieldType = resolvedType.asReferenceType().getQualifiedName();
                    } else if (resolvedType.isPrimitive()) {
                        fieldType = resolvedType.asPrimitive().name();
                    }
                    System.out.println("resolved " + resolved + " = " + fieldType);
                }
            }
        }
    }
}
