package org.dreamcat.cli.generator.apidoc;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author Jerry Will
 * @version 2022-03-16
 */
public class ApiDocGeneratorPlugin implements Plugin<Project> {

    private static final String name = "apidocGenerate";
    // private static final String taskGroup = "documentation";

    @Override
    public void apply(Project project) {
        // ApiDocGeneratorExtension extension =
        project.getExtensions().create(name, ApiDocGeneratorExtension.class);

        // project.getTasks().create(name, ApiDocGeneratorTask.class, project, extension);
        // Task task =
        project.getTasks().create(name, ApiDocGeneratorTask.class);
        // task.setGroup(taskGroup);
    }
}
