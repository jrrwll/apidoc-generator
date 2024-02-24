package org.dreamcat.cli.generator.apidoc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.maven.project.MavenProject;

/**
 * @author Jerry Will
 * @version 2024-02-24
 */
public class MavenUtil {

    public static List<String> getCompileClasspath(MavenProject project) throws Exception {
        return project.getCompileClasspathElements();
    }

    public static List<String> getRuntimeClasspath(MavenProject project) throws Exception {
        return project.getRuntimeClasspathElements();
    }

    public static File getClassDir(MavenProject project) throws IOException {
        File basedir = getBaseDir(project);
        if (basedir == null) {
            basedir = new File(".").getCanonicalFile();
        }
        File file = new File(basedir, project.getBuild().getOutputDirectory());
        if (file.exists()) return file;
        return new File(basedir, "target/classes");
    }

    public static File getSrcDir(MavenProject project) throws IOException {
        File basedir = getBaseDir(project);
        if (basedir == null) {
            basedir = new File(".").getCanonicalFile();
        }
        File file = new File(basedir, project.getBuild().getSourceDirectory());
        if (file.exists()) return file;
        return new File(basedir, "src/main/java");
    }

    public static @Nullable File getBaseDir(MavenProject project) {
        File basedir;
        while ((basedir = project.getBasedir()) == null) {
            project = project.getParent();
            if (project == null) break;
        }
        return basedir;
    }
}
