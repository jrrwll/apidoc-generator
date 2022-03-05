package org.dreamcat.cli.generator.apidoc.javadoc;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jerry Will
 * @version 2021-12-16
 */
@Slf4j
@RequiredArgsConstructor
public class FieldCommentProvider implements Function<Field, String> {

    private final List<String> srcDirs;
    private final List<String> basePackages;

    private final Map<Class<?>, CommentClassDef> classCache = new ConcurrentHashMap<>();

    @Override
    public String apply(Field field) {
        Class<?> declaringClass = field.getDeclaringClass();
        if (!belongBasePackages(declaringClass.getPackage().getName())) return null;

        CommentClassDef classDef = classCache.get(declaringClass);
        if (classDef == null) {
            classDef = parseCommentClassDef(declaringClass);
        }
        if (classDef != null) {
            classCache.putIfAbsent(declaringClass, classDef);
        } else {
            log.warn("class {} is not found in source dirs {}", declaringClass, srcDirs);
            return null;
        }

        CommentFieldDef fieldDef = classDef.getFields().stream()
                .filter(it -> it.getName().equals(field.getName())).findAny().orElse(null);
        if (fieldDef == null) {
            log.warn("field {} is not found in source dirs {}", field, srcDirs);
            return null;
        }
        return fieldDef.getComment();
    }

    private CommentClassDef parseCommentClassDef(Class<?> clazz) {
        String className = clazz.getName(), current = className;
        char sep = File.separatorChar;
        for (String srcDir : srcDirs) {
            while (belongBasePackages(current)) {
                File file = new File(srcDir, current.replace('.', sep) + ".java");
                if (file.exists()) {
                    return CommentJavaParser.parseOne(file, srcDirs, className);
                }
                current = current.substring(0, current.lastIndexOf('.'));
            }
        }
        return null;
    }

    private boolean belongBasePackages(String name) {
        for (String basePackage : basePackages) {
            if (name.startsWith(basePackage)) return true;
        }
        return false;
    }
}
