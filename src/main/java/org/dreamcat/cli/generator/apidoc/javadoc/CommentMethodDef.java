package org.dreamcat.cli.generator.apidoc.javadoc;

import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.Data;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2021-12-09
 */
@Data
public class CommentMethodDef {

    private String name;
    private String comment;
    private List<CommentParameterDef> parameters;
    private String returnComment;

    public CommentMethodDef(MethodDeclaration declaration) {
        this.name = declaration.getName().getIdentifier();
        String methodComment = JavaParserUtil.getComment(declaration);

        List<String> comments = new ArrayList<>();
        Map<String, String> paramComments = new HashMap<>();

        for (String line : methodComment.split("\n")) {
            if (ObjectUtil.isBlank(line = StringUtil.trimLeft(line, " *\r\t"))) continue;
            if (!line.startsWith("@")) {
                comments.add(line);
            } else if (line.startsWith("@param")) {
                String[] words = line.split("[ \t]+", 3);
                if (words.length == 3) {
                    paramComments.put(words[1], words[2].trim());
                }
            } else if (line.startsWith("@return")) {
                this.returnComment = line.substring(7).trim();
            }
        }
        if (!comments.isEmpty()) {
            this.comment = String.join("\n", comments);
        }

        this.parameters = declaration.getParameters().stream()
                .map(it -> {
                    CommentParameterDef param = new CommentParameterDef(it);
                    if (ObjectUtil.isBlank(param.getComment())) {
                        param.setComment(paramComments.getOrDefault(param.getName(), ""));
                    }
                    return param;
                })
                .collect(Collectors.toList());
    }

}
