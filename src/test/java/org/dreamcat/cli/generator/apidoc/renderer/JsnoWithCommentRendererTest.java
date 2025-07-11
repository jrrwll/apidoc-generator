package org.dreamcat.cli.generator.apidoc.renderer;

import com.example.biz.base.ApiPage;
import com.example.biz.base.ApiResult;
import com.example.biz.result.ComplexModel;
import org.dreamcat.common.json.JSON;
import org.dreamcat.common.reflect.ObjectRandomGenerator;
import org.dreamcat.common.reflect.ObjectType;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-07-11
 */
class JsnoWithCommentRendererTest {

    @Test
    void test() {
        char indentPrefix = (char) 9492;
        System.out.println(indentPrefix);
    }

    @Test
    void testRandom() {
        ObjectRandomGenerator randomGenerator = new ObjectRandomGenerator();

        ObjectType type = ObjectType.fromType(ApiResult.class,
                ObjectType.fromType(ApiPage.class, ComplexModel.class));
        Object obj = randomGenerator.generate(type);
        System.out.println(obj);
        System.out.println(JSON.stringifyWithComment(obj));
    }
}
