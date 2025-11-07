package org.dreamcat.cli.generator.apidoc.renderer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.hc.httpclient.HttpClientUtil;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ObjectUtil;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * only POST is supported
 *
 * @author Jerry Will
 * @version 2025-09-12
 */
@Data
@Slf4j
@JsonInclude(Include.NON_EMPTY)
public class HttpPushConfig {

    private String url;
    private Map<String, String> headers;
    private Object json; // application/json
    private String text; // text/plain
    private Map<String, String> form; // application/x-www-form-urlencoded

    public void pushDoc(Map<String, String> context) throws IOException {
        log.info("prepare to push doc to {}", url);
        if (url == null) {
            log.error("url is unset, skip to push doc: {}", JsonUtil.toJson(this));
            return;
        }
        if (json == null && ObjectUtil.isBlank(text) && ObjectUtil.isEmpty(form)) {
            log.error("json|text|from is unset, skip to push doc: {}", JsonUtil.toJson(this));
            return;
        }

        String formatted_url = InterpolationUtil.format(url, context);
        if (headers != null) {
            headers = headers.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
                    entry -> InterpolationUtil.format(entry.getValue(), context)));
        }

        String resp;
        if (json != null) {
            String jsonStr = JsonUtil.toJson(json);
            String formatted_json = InterpolationUtil.format(jsonStr, context);
            if (log.isDebugEnabled()) {
                log.debug("push doc to {} with json:\n{}", formatted_url, formatted_json);
            }
             resp = HttpClientUtil.postJson(formatted_url, headers, formatted_json);
        } else if (ObjectUtil.isNotEmpty(form)) {
            Map<String, String> formatted_form = form.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
                    entry -> InterpolationUtil.format(entry.getValue(), context)));
            if (log.isDebugEnabled()) {
                log.debug("push doc to {} with form:\n{}", formatted_url, JsonUtil.toJson(formatted_form));
            }
            resp = HttpClientUtil.postForm(formatted_url, headers, formatted_form);
        } else {
            String formatted_text = InterpolationUtil.format(text, context);
            if (log.isDebugEnabled()) {
                log.debug("push doc to {} with text:\n{}", formatted_url, formatted_text);
            }
            resp = HttpClientUtil.postString(formatted_url, headers, formatted_text);
        }
        log.info("finished to push doc, response:\n{}", resp);
    }
}
