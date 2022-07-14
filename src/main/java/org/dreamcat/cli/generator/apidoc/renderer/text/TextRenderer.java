package org.dreamcat.cli.generator.apidoc.renderer.text;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import org.dreamcat.cli.generator.apidoc.renderer.ApiDocRenderer;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.io.ClassPathUtil;
import org.dreamcat.common.util.ReflectUtil;
import org.dreamcat.common.x.jackson.JsonUtil;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
@Setter
public abstract class TextRenderer implements ApiDocRenderer {

    protected transient String template;

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

    public abstract String getTemplate();

    @Override
    public void render(ApiDoc apiDoc, Writer out) {
        Map<String, Object> context = JsonUtil.toMap(apiDoc);
        List<Field> fields = ReflectUtil.retrieveNoStaticFields(getClass());
        for (Field field : fields) {
            if (Modifier.isTransient(field.getModifiers())) continue;
            context.put(field.getName(), ReflectUtil.getValue(this, field));
        }

        TemplateUtil.process(getTemplate(), context, out,
                Collections.singletonMap("fields", TEMPLATE_FIELDS));
    }

    private static final String TEMPLATE_FIELDS;

    static {
        try {
            TEMPLATE_FIELDS = ClassPathUtil.getResourceAsString(
                    "org/dreamcat/cli/generator/apidoc/IndentedTable-fields.ftl");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
