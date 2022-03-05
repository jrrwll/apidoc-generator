package org.dreamcat.cli.generator.apidoc.javadoc;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
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

        ResolvedTypeDeclaration declaringType = resolved.declaringType();
        this.type = declaringType.getQualifiedName();

    }
}
