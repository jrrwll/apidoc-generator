package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import org.dreamcat.common.net.UrlUtil;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * @author Jerry Will
 * @version 2022-04-14
 */
public class GradleUtil {

    /**
     * dependsOn some tasks
     * 
     * @see JavaPlugin#JAR_TASK_NAME
     * @see JavaPlugin#COMPILE_JAVA_TASK_NAME
     */
    public static void dependsOn(Task task, String... taskNames) {
        Project project = task.getProject();
        
        for (String taskName : taskNames) {
            Task t = project.getTasks().getByName(taskName);
            task.dependsOn(t);
        }
    }

    // apply plugin: 'java'
    public static void applyJavaPlugin(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public static URLClassLoader buildUserCodeClassLoader(Project project) {
        URL[] urls = getUserCodeClassPaths(project);
        return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
    }

    public static URL[] getUserCodeClassPaths(Project project) {
        return Stream.of(getClassDirs(project),
                        getCompileClasspath(project))
                .flatMap(Collection::stream).map(UrlUtil::toURL).toArray(URL[]::new);
    }

    public static Set<File> getCompileClasspath(Project project) {
        Configuration compileConfiguration = project.getConfigurations()
                .getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
        return compileConfiguration.getFiles();
    }

    public static Set<File> getRuntimeClasspath(Project project) {
        Configuration compileConfiguration = project.getConfigurations()
                .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
        return compileConfiguration.getFiles();
    }

    public static Set<File> getClassDirs(Project project) {
        SourceSetContainer container = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        FileCollection classesDirs = container.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput().getClassesDirs();
        return classesDirs.getFiles();
    }

    public static Set<File> getSrcDirs(Project project) {
        SourceSetContainer container = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        return container.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getAllJava().getSrcDirs();
    }

}
