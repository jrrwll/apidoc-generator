> see [more](https://jrrwll.github.io/docs/tool/apidoc-generator/) on my [Blog](https://jrrwll.github.io/)

## maven plugin

### build

```shell
export GPG_PASSPHRASE=xxx
mvn install -Dmaven.test.skip=true
```

### usage

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

## doxygen

```shell
brew install doxygen
```
