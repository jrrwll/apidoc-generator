package org.dreamcat.cli.generator.apidoc.javadoc;

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class CommentClassDef {

    private String type;
    private String comment;
    private List<CommentFieldDef> fields;
    private List<CommentMethodDef> methods;

    public CommentClassDef(TypeDeclaration<?> declaration) {
        ResolvedReferenceTypeDeclaration resolve = declaration.resolve();
        this.type = resolve.getQualifiedName();
        this.comment = JavaParserUtil.getJavadocComment(declaration);

        this.fields = declaration.getFields().stream()
                .filter(it -> !it.isStatic()) // no-static
                .map(CommentFieldDef::new)
                .collect(Collectors.toList());

        this.methods = declaration.getMethods().stream()
                .filter(it -> !it.isStatic()) // no-static
                .map(CommentMethodDef::new)
                .collect(Collectors.toList());
    }
}
