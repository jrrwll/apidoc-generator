> see [more](https://jrrwll.github.io/docs/tool/apidoc-generator/) on my [Blog](https://jrrwll.github.io/)

## maven plugin

### build

```shell
export GPG_PASSPHRASE=xxx
mvn install -Dmaven.test.skip=true

# or skip sign
mvn install -Dmaven.test.skip=true -Dgpg.skip=true

# install gpg before deploy
brew install gnupg
# or linux
sudo apt-get install gnupg
# or windows
# https://www.gpg4win.org/
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
