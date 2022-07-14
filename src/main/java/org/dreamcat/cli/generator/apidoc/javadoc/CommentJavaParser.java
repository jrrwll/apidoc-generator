package org.dreamcat.cli.generator.apidoc.javadoc;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.cli.generator.apidoc.ApiDocConfig;

/**
 * @author Jerry Will
 * @version 2021-12-16
 */
@Slf4j
@RequiredArgsConstructor
public class CommentJavaParser {

    private final List<String> srcDirs;
    private final List<String> basePackages;

    private final Map<Class<?>, CommentClassDef> classCache = new ConcurrentHashMap<>();
    private final Map<Field, CommentFieldDef> fieldCache = new ConcurrentHashMap<>();

    public CommentJavaParser(ApiDocConfig config) {
        this(config.getSrcDirs(), config.getBasePackages());
    }

    public String provideFieldComment(Field field) {
        CommentFieldDef fieldDef = resolveField(field);
        return fieldDef != null ? fieldDef.getComment() : null;
    }

    public CommentFieldDef resolveField(Field field) {
        return fieldCache.computeIfAbsent(field, this::parseField);
    }

    private CommentFieldDef parseField(Field field) {
        Class<?> declaringClass = field.getDeclaringClass();
        if (!belongBasePackages(getPackageName(declaringClass))) return null;

        CommentClassDef classDef = classCache.get(declaringClass);
        if (classDef == null) {
            classDef = parseClass(declaringClass);
        }
        if (classDef != null) {
            classCache.putIfAbsent(declaringClass, classDef);
        } else {
            log.warn("{} is not found in source dirs {}", declaringClass, srcDirs);
            return null;
        }

        String fieldName = field.getName();
        CommentFieldDef fieldDef = classDef.getFields().stream()
                .filter(it -> it.getName().equals(fieldName)).findAny().orElse(null);
        if (fieldDef == null) {
            log.warn("{} is not found in source dirs {}", field, srcDirs);
            return null;
        }
        return fieldDef;
    }

    public CommentClassDef resolveClass(Class<?> clazz) {
        return classCache.computeIfAbsent(clazz, this::parseClass);
    }

    private CommentClassDef parseClass(Class<?> clazz) {
        String className = clazz.getName(), current = className;
        char sep = File.separatorChar;
        for (String srcDir : srcDirs) {
            while (belongBasePackages(current)) {
                int dollar = current.lastIndexOf('$');
                String name = dollar == -1 ? current : current.substring(0, dollar);
                File file = new File(srcDir, name.replace('.', sep) + ".java");
                if (file.exists()) {
                    return CommentClassDef.parseOne(file, srcDirs, className);
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

    private String getPackageName(Class<?> clazz) {
        String name = clazz.getName();
        int i = name.lastIndexOf('.');
        if (i != -1) {
            return name.substring(0, i);
        } else {
            return ""; // not null
        }
    }

    public void clear() {
        classCache.clear();
        fieldCache.clear();
    }
}
