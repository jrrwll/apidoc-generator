package org.dreamcat.cli.generator.apidoc;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApiDocGeneratorPlugin implements Plugin<Project> {

    private static final String name = "apidocGenerate";
    private static final String taskGroup = "documentation";

    @Override
    public void apply(Project project) {
        if (!project.getPlugins().hasPlugin("java") &&
                !project.getPlugins().hasPlugin("java-library")) {
            throw new GradleException(
                    "Plugin 'org.dreamcat.apidoc-generator' requires the 'java' or 'java-library' plugin to be applied first.");
        }
        // project.getPluginManager().apply("java");

        // Property<?> or getter/setter pojo
        project.getExtensions().create(name, ApiDocGeneratorExtension.class);

        // project.getTasks().create(name, ApiDocGeneratorTask.class, project, extension);
        Task task = project.getTasks().create(name, ApiDocGeneratorTask.class);
        task.setGroup(taskGroup);

        task.dependsOn(project.getTasks().named(JavaPlugin.CLASSES_TASK_NAME));
    }
}
