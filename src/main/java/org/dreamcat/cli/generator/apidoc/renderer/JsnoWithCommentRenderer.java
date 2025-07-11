package org.dreamcat.cli.generator.apidoc.renderer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.cli.generator.apidoc.scheme.ApiFunction;
import org.dreamcat.cli.generator.apidoc.scheme.ApiGroup;
import org.dreamcat.cli.generator.apidoc.scheme.ApiInputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiOutputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiParamField;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
@Getter
@Setter
@Accessors(chain = true)
@JsonInclude(Include.NON_EMPTY)
public class JsnoWithCommentRenderer implements ApiDocRenderer {

    // output style
    boolean fieldsNoRequired;
    boolean outputParamAsIndentedTable;

    private String nameHeader = "###";
    private String functionHeader = "####";
    private String inputParamTitle = "**Input Params**";
    private String outputParamTitle = "**Output Param**";
    private boolean pinFunctionComment;
    private String seqPrefix;
    // indentedTable
    private int maxNestLevel = 4; // [0, 7]
    private String indentSpace = "&nbsp;&nbsp;";
    private String indentPrefix = String.valueOf((char) 9492); // └
    private String indentName = "Name";
    private String indentType = "Type";
    private String indentRequired = "Required";
    private String indentComment = "Comment";
    private String requiredNull = "-";
    private String requiredTrue = "Y";
    private String requiredFalse = "N";

    @Override
    public void render(ApiDoc doc, Writer out) throws IOException {
        if (ObjectUtil.isNotBlank(doc.getName())) {
            out.write(nameHeader);
            out.write(" ");
            out.write(doc.getName());
            if (ObjectUtil.isNotBlank(doc.getVersion())) {
                out.write(" (version=");
                out.write(doc.getVersion());
                out.write(")");
            }
            out.write("\n");
            if (ObjectUtil.isNotBlank(doc.getComment())) {
                out.write("> ");
                out.write(doc.getComment());
                out.write("\n");
            }
            out.write("\n");
        }
        int seq = 0;
        List<ApiGroup> groups = doc.getGroups();
        for (int i = 0, m = groups.size(); i < m; i++) {
            List<ApiFunction> functions = groups.get(i).getFunctions();
            for (int j = 0, n = functions.size(); j < n; j++) {
                if (i != 0 || j != 0) {
                    out.write("\n");
                }
                renderFunction(functions.get(j), ++seq, out);
            }
        }
    }

    protected void renderFunction(ApiFunction function, int seq, Writer out) throws IOException {
        out.write(functionHeader);
        if (ObjectUtil.isNotEmpty(seqPrefix)) {
            out.write(" ");
            out.write(seqPrefix);
            out.write(String.valueOf(seq));
        }
        out.write(" ");
        if (pinFunctionComment && ObjectUtil.isNotBlank(function.getComment())) {
            out.write(function.getComment());
        } else {
            out.write(function.getName());
        }

        if (ObjectUtil.isNotEmpty(function.getPath())) {
            out.write(" `");
            out.write(String.join("`, `", function.getPath()));
            out.write("`");
        }
        out.write("\n");
        if (!pinFunctionComment && ObjectUtil.isNotBlank(function.getComment())) {
            out.write("\n> ");
            out.write(function.getComment());
            out.write("\n");
        }
        out.write("\n");

        if (ObjectUtil.isNotEmpty(inputParamTitle)) {
            out.write(inputParamTitle);
            out.write("\n\n");
        }
        if (function.isInputParamsMerged()) {
            List<ApiParamField> fields = function.getInputParams().get(0).getFields();
            rendererIndentedTable(fields, out);
            out.write("\n");
        } else {
            for (ApiInputParam inputParam : function.getInputParams()) {
                if (function.getInputParamCount() > 1) {
                    out.write("- *");
                    out.write(inputParam.getName());
                    out.write("*");
                    if (ObjectUtil.isNotBlank(inputParam.getComment())) {
                        out.write(" ");
                        out.write(inputParam.getComment());
                    }
                    out.write("\n\n");
                }
                out.write("```js\n");
                out.write(inputParam.getJsonWithComment());
                if (function.getInputParamCount() == 1 &&
                        ObjectUtil.isNotBlank(inputParam.getComment())) {
                    out.write(" // ");
                    out.write(inputParam.getComment());
                }
                out.write("\n```\n\n");
            }
        }

        ApiOutputParam outputParam = function.getOutputParam();
        if (ObjectUtil.isNotEmpty(outputParamTitle)) {
            out.write(outputParamTitle);
            out.write("\n\n");
        }
        if (outputParamAsIndentedTable) {
            List<ApiParamField> fields = function.getOutputParam().getFields();
            rendererIndentedTable(fields, out);
        } else {
            out.write("```js\n");
            out.write(outputParam.getJsonWithComment());
            if (ObjectUtil.isNotBlank(outputParam.getComment())) {
                out.write(" // ");
                out.write(outputParam.getComment());
            }
            out.write("\n```\n");
        }
    }

    protected void rendererIndentedTable(
            List<ApiParamField> fields, Writer out) throws IOException {
        out.write("| ");
        out.write(indentName);
        out.write(" | ");
        out.write(indentType);
        if (!fieldsNoRequired) {
            out.write(" | ");
            out.write(indentRequired);
        }
        out.write(" | ");
        out.write(indentComment);
        out.write(" |\n");
        if (fieldsNoRequired) {
            out.write("| :- | :- | :- |\n");
        } else {
            out.write("| :- | :- | :- | :- |\n");
        }
        for (ApiParamField field : fields) {
            rendererIndentedTableField(field, 0, out);
        }
    }

    private void rendererIndentedTableField(ApiParamField field, int nestLevel, Writer out) throws IOException {
        out.write("|");
        out.write(StringUtil.repeat(indentSpace, nestLevel + 1));
        out.write(indentPrefix);
        out.write(" ");
        out.write(field.getName());
        out.write(" | ");
        out.write(field.getTypeName());
        if (!fieldsNoRequired) {
            out.write(" | ");
            out.write(
                    field.getRequired() == null ? requiredNull : (field.getRequired() ? requiredTrue : requiredFalse));
        }
        out.write(" | ");
        out.write(field.getComment());
        out.write(" |\n");

        List<ApiParamField> subFields = field.getFields();
        if (nestLevel < maxNestLevel && ObjectUtil.isNotEmpty(subFields)) {
            nestLevel++;
            for (ApiParamField subField : subFields) {
                rendererIndentedTableField(subField, nestLevel, out);
            }
        }
    }
}
