package org.dreamcat.cli.generator.apidoc;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jerry Will
 * @version 2024-02-24
 */
public class MavenUtil {

    public static List<String> getCompileClasspath(MavenProject project) throws Exception {
        // target/classes
        return project.getCompileClasspathElements();
    }

    public static List<String> getRuntimeClasspath(MavenProject project) throws Exception {
        // target/classes
        return project.getRuntimeClasspathElements();
    }

    public static String getClassDir(MavenProject project) throws IOException {
        // target/classes
        return project.getBuild().getOutputDirectory();
    }

    public static String getSrcDir(MavenProject project) throws IOException {
        // src/main/java
        return project.getBuild().getSourceDirectory();
    }

    public static File getBaseDir(MavenProject project) {
        File basedir;
        while ((basedir = project.getBasedir()) == null) {
            project = project.getParent();
            if (project == null) break;
        }
        return basedir;
    }

    public static List<File> getDependencies(
            MavenProject project, ArtifactRepository localRepository) {
        String repoBaseDir = localRepository.getBasedir();
        return project.getDependencies().stream()
                .filter(dep -> "jar".equals(dep.getType()))
                .map(dep -> new File(repoBaseDir, getPath(dep)))
                .collect(Collectors.toList());
    }

    private static String getPath(Dependency dep) {
        return dep.getGroupId().replace('.', File.separatorChar) +
                File.separatorChar + dep.getArtifactId() + File.separatorChar +
                dep.getVersion() + File.separatorChar +
                dep.getArtifactId() + "-" + dep.getVersion() + ".jar";
    }
}
