package org.dreamcat.cli.generator.apidoc.renderer;

import java.io.Writer;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.dreamcat.cli.generator.apidoc.scheme.ApiDoc;
import org.dreamcat.common.json.JsonUtil;

/**
 * @author Jerry Will
 * @version 2024-01-08
 */
@Data
public class SimpleRenderer implements ApiDocRenderer {

    int a;
    Double b;
    boolean c;
    List<String> d;
    Map<String, Object> e;

    @Override
    public void render(ApiDoc doc, Writer out) {
        System.out.println("*** output of simple renderer plugin *** ");
        System.out.println("this: " + JsonUtil.toJson(this));
        pushDoc(doc);
        System.out.println("******             end            ******");
    }

    /**
     * Note: Oops!!! using {@link org.dreamcat.common.hc.httpclient.HttpClientWget} will cause
     * {@link NoClassDefFoundError} on {@link org.apache.hc.client5.http.classic.methods.HttpUriRequest}
     * So we have to handle hc stuff manually
     *
     * @param doc doc
     */
    @SneakyThrows
    private void pushDoc(ApiDoc doc) {
        System.out.println(JsonUtil.toJson(doc));

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            client.execute(new HttpGet("https://exmaple.com/"), resp -> {
                String bodyStr = EntityUtils.toString(resp.getEntity());
                if (resp.getCode() == 200) {
                    System.out.println(bodyStr);
                } else {
                    System.err.printf("failed to curl: code=%d, msg=%s, body=%s\n",
                            resp.getCode(), resp.getReasonPhrase(), bodyStr);
                }
                return null;
            });
        }
    }
}
