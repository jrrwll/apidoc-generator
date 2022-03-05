package org.dreamcat.cli.generator.apidoc.renderer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig;
import org.dreamcat.cli.generator.apidoc.javadoc.FieldCommentProvider;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.cli.generator.apidoc.scheme.ApiFunction;
import org.dreamcat.cli.generator.apidoc.scheme.ApiGroup;
import org.dreamcat.cli.generator.apidoc.scheme.ApiInputParam;
import org.dreamcat.cli.generator.apidoc.scheme.ApiOutputParam;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.text.DollarInterpolation;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.databind.instance.RandomInstance;
import org.dreamcat.databind.json.legacy.JSONWithComment;
import org.dreamcat.databind.type.ObjectType;

/**
 * @author Jerry Will
 * @version 2021-12-16
 */
@Setter
public class JsonWithCommentRenderer implements ApiDocRenderer<String> {

    private String functionSep = "\n";
    private String groupSep = "\n\n";
    private String inputParamSep = "";
    private String inputParamTitle;
    private String outputParamTitle;

    private String seqFormat = "%d";
    private Function<ApiFunction, String> functionTitleFormatter = ApiFunction::getComment;
    private Function<ObjectType, String> bodyFormatter = this::formatBody;

    private Function<FunctionTemplate, String> functionRenderer = this::renderFunction;
    private Function<FunctionInputParamTemplate, String> inputParamRenderer = this::renderInputParam;
    private Function<FunctionOutputParamTemplate, String> outputParamRenderer = this::renderOutputParam;

    private final FieldCommentProvider fieldCommentProvider;
    private final RandomInstance randomInstance = new RandomInstance();

    public JsonWithCommentRenderer(ApiDocConfig config) {
        this.fieldCommentProvider = new FieldCommentProvider(config.getSrcDirs(), config.getBasePackages());
    }

    public File renderAndSave(ApiDoc apiDoc, String outputPath) throws IOException {
        String doc = render(apiDoc);

        File file = Companion.getOutputPath(outputPath, "md");
        FileUtil.writeFrom(file, doc);
        return file;
    }

    @Override
    public String render(ApiDoc apiDoc) {
        List<String> groupTemplates = new ArrayList<>();
        int seq = 1;
        for (ApiGroup group : apiDoc.getGroups().values()) {
            List<String> functionTemplates = new ArrayList<>();
            for (ApiFunction function : group.getFunctions().values()) {
                FunctionTemplate ft = formatFunctionTemplate(function, seq++);
                functionTemplates.add(functionRenderer.apply(ft));
            }
            groupTemplates.add(String.join(functionSep, functionTemplates));
        }
        return String.join(groupSep, groupTemplates);
    }

    public FunctionTemplate formatFunctionTemplate(ApiFunction apiFunction, int seq) {
        FunctionTemplate ft = new FunctionTemplate();
        ft.seq = String.format(seqFormat, seq);
        ft.title = functionTitleFormatter.apply(apiFunction);

        // input & output
        ft.inputParams = apiFunction.getInputParams().values().stream()
                .map(this::formatInputParamTemplate)
                .collect(Collectors.toList());
        ft.outputParam = formatOutputParamTemplate(apiFunction.getOutputParam());

        // http fields
        List<String> list;
        if (ObjectUtil.isNotEmpty(list = apiFunction.getPath())) {
            ft.path = String.join("|", list);
        }
        if (ObjectUtil.isNotEmpty(list = apiFunction.getAction())) {
            ft.action = String.join("|", list);
        }
        return ft;
    }

    public FunctionInputParamTemplate formatInputParamTemplate(ApiInputParam apiInputParam) {
        FunctionInputParamTemplate pt = new FunctionInputParamTemplate();
        String name = apiInputParam.getName();
        String comment = apiInputParam.getComment();
        String title = "";
        if (ObjectUtil.isNotBlank(name) && ObjectUtil.isNotBlank(comment)) {
            title = name + " " + comment + "\n";
        } else if (ObjectUtil.isNotBlank(name)) {
            title = name + "\n";
        } else if (ObjectUtil.isNotBlank(comment)) {
            title = comment + "\n";
        }
        pt.title = title;
        pt.body = bodyFormatter.apply(apiInputParam.getType());
        pt.required = apiInputParam.getRequired();
        return pt;
    }

    public FunctionOutputParamTemplate formatOutputParamTemplate(ApiOutputParam apiOutputParam) {
        FunctionOutputParamTemplate pt = new FunctionOutputParamTemplate();
        pt.body = bodyFormatter.apply(apiOutputParam.getType());
        return pt;
    }

    public String formatBody(ObjectType type) {
        Object bean = randomInstance.randomValue(type);
        return JSONWithComment.stringify(bean, fieldCommentProvider);
    }

    public String renderFunction(FunctionTemplate ft) {
        // input & output
        String input = ft.getInputParams().stream()
                .map(inputParamRenderer)
                .collect(Collectors.joining(inputParamSep));
        String output = outputParamRenderer.apply(ft.getOutputParam());

        // http fields
        String path = ft.getPath();
        if (ObjectUtil.isBlank(path)) {
            path = "";
        } else {
            String action = ft.getAction();
            if (ObjectUtil.isBlank(action)) {
                path = " " + path;
            } else {
                path = " " + action + " " + path;
            }
        }

        return DollarInterpolation.format(template_all, MapUtil.of(
                "seq", ft.getSeq(),
                "title", ft.getTitle(),
                "path", path,
                "input", input,
                "output", output), "");
    }

    public String renderInputParam(FunctionInputParamTemplate pt) {
        if (ObjectUtil.isNotBlank(inputParamTitle)) pt.setTitle(inputParamTitle + "\n");
        return DollarInterpolation.format(template_param, MapUtil.of(
                "title", pt.getTitle(),
                "body", pt.getBody()), "");
    }

    public String renderOutputParam(FunctionOutputParamTemplate pt) {
        String title = ObjectUtil.isNotBlank(outputParamTitle) ? outputParamTitle + "\n" : "";
        return DollarInterpolation.format(template_param, MapUtil.of(
                "title", title,
                "body", pt.getBody()), "");
    }

    @Data
    public static class FunctionTemplate {

        private String groupSeq;
        private String seq;
        private String title;
        // input & output
        private List<FunctionInputParamTemplate> inputParams;
        private FunctionOutputParamTemplate outputParam;
        // http restful
        private String path;
        private String action;
    }

    @Data
    @NoArgsConstructor
    public static class FunctionInputParamTemplate {

        private String title;
        private String body; // json-with-comment
        // http restful
        private Boolean required;
    }

    @Data
    @NoArgsConstructor
    public static class FunctionOutputParamTemplate {

        private String body; // json-with-comment
    }

    private static final String template_all = "$seq $title$path \n"
            + "$input\n"
            + "$output";
    private static final String template_param = "$title```js\n"
            + "$body\n"
            + "```\n";
}
