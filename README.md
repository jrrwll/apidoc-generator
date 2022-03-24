> see [more](https://jrrwll.github.io/docs/tool/apidoc-generator/) on my [Blog](https://jrrwll.github.io/)

### ops log

```shell
# create a maven-plugin project
mvn archetype:generate \
    -DgroupId=org.dreamcat \
    -DartifactId=apidoc-generator-maven-plugin \
    -DarchetypeArtifactId=maven-archetype-mojo \
    -DinteractiveMode=false

# use the plugin
mvn archetype:generate \
    -DgroupId=org.dreamcat \
    -DartifactId=plugin-maven-example \
    -DinteractiveMode=false
```

```shell
mvn org.dreamcat:apidoc-generator-maven-plugin:apidocGenerate
```

or add shorthand to `~/.m2/settings.xml`

```xml
<pluginGroups>
    <pluginGroup>org.dreamcat</pluginGroup>
  </pluginGroups>
```

```shell
mvn apidoc-generator:apidocGenerate
```
