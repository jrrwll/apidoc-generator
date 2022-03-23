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

mvn org.dreamcat:apidoc-generator-maven-plugin:apidocGenerate
```