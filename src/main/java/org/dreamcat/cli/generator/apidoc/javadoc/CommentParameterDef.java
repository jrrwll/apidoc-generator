package org.dreamcat.cli.generator.apidoc.javadoc;

import com.github.javaparser.ast.body.Parameter;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class CommentParameterDef {

    private String name;
    private String comment;
    // private String type; // maybe fail to resolve, use reflect pkg instead

    public CommentParameterDef(Parameter declaration) {
        this.name = declaration.getName().getIdentifier();
        this.comment = JavaParserUtil.getJavadocComment(declaration);
        // ResolvedParameterDeclaration resolved = declaration.resolve();
        // this.type = resolved.describeType();
    }
}
