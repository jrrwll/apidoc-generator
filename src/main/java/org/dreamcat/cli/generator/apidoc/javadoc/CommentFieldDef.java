package org.dreamcat.cli.generator.apidoc.javadoc;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class CommentFieldDef {

    private String name;
    private String comment;
    private String type;

    public CommentFieldDef(FieldDeclaration declaration) {
        ResolvedFieldDeclaration resolved = declaration.resolve();
        this.name = resolved.getName();
        this.comment = JavaParserUtil.getJavadocComment(declaration);
        // ResolvedType resolvedType = resolved.getType();
        // if (resolvedType.isReferenceType()) {
        //     this.type = resolvedType.asReferenceType().getQualifiedName();
        // } else if (resolvedType.isPrimitive()) {
        //     this.type = resolvedType.asPrimitive().name();
        // }
    }
}
