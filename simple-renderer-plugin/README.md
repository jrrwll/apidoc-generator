## test the plugin

```shell
project_name=simple-renderer-plugin
./gradlew :simple-renderer-plugin:jar
./gradlew :simple-renderer-plugin:copyDep

cp $project_name/build/libs/*.jar $project_name/build/dep/
```

