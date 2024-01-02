package org.dreamcat.cli.generator.apidoc.renderer;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.ClassPathUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ReflectUtil;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
@Setter
@Accessors(chain = true)
@RequiredArgsConstructor
public class TextTemplateRenderer implements ApiDocRenderer {

    private final String template;
    private final Map<String, String> includeTemplates;

    private String nameHeader = "##";
    private String functionHeader = "###";
    private String paramHeader = "####";
    private String inputParamTitle = "- *Input Params*";
    private String outputParamTitle = "- *Output Param*";
    private boolean pinFunctionComment;
    private String seqPrefix;

    // indentedTable
    private int maxNestLevel = 4; // [0, 7]
    private String indentPrefix = String.valueOf((char) 9492); // └
    private String indentName = "Name";
    private String indentType = "Type";
    private String indentRequired = "Required";
    private String indentComment = "Comment";
    private String requiredTrue = "Y";
    private String requiredFalse = "N";

    public TextTemplateRenderer(String template) {
        this(template, Collections.emptyMap());
    }

    @Override
    public void render(ApiDoc apiDoc, Writer out) {
        Map<String, Object> context = JsonUtil.toMap(apiDoc);
        List<Field> fields = ReflectUtil.retrieveBeanFields(getClass());
        for (Field field : fields) {
            context.put(field.getName(), ReflectUtil.getValue(this, field));
        }
        TemplateUtil.process(template, context, out, includeTemplates);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String template = TEMPLATE;
        private final Map<String, String> includeTemplates = MapUtil.of("fields", FIELDS_TEMPLATE);

        public Builder jsnoWithComment() {
            this.template = JWC_TEMPLATE;
            return this;
        }

        public Builder fieldsNoRequired() {
            includeTemplates.put("fields", FIELDS_NO_REQUIRED_TEMPLATE);
            return this;
        }

        public TextTemplateRenderer build() {
            return new TextTemplateRenderer(template, includeTemplates);
        }
    }

    private static final String TEMPLATE;
    private static final String JWC_TEMPLATE;
    private static final String FIELDS_TEMPLATE;
    private static final String FIELDS_NO_REQUIRED_TEMPLATE;

    static {
        try {
            final String backPkg = "org/dreamcat/cli/generator/apidoc";
            TEMPLATE = ClassPathUtil.getResourceAsString(
                    backPkg + "/IndentedTable.ftl");
            JWC_TEMPLATE = ClassPathUtil.getResourceAsString(
                    backPkg + "/JsonWithComment.ftl");
            FIELDS_TEMPLATE = ClassPathUtil.getResourceAsString(
                    backPkg + "/IndentedTable-fields.ftl");
            FIELDS_NO_REQUIRED_TEMPLATE = ClassPathUtil.getResourceAsString(
                    backPkg + "/IndentedTable-fields-no-required.ftl");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
