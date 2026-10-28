# mixin-libs

这里需要放一个**第三方 jar**（不随仓库分发）：

```
mixin-libs/mixin-processor.jar        # Mixin 0.8.5 的 annotation processor
```

`build.gradle` 里这样用它（Sponge 仓库的 pom 是 404，所以走本地文件）：

```groovy
annotationProcessor files('mixin-libs/mixin-processor.jar')
```

## 怎么拿到

任选其一：

1. **从 Sponge 仓库下载**：<https://repo.spongepowered.org/repository/maven-public/org/spongepowered/mixin/0.8.5/>
   取 `mixin-0.8.5-processor.jar`，重命名成 `mixin-processor.jar`。
2. **从 Gradle 缓存里捞**：跑过任何 MixinGradle 项目之后，
   `~/.gradle/caches/modules-2/files-2.1/org.spongepowered/mixin/0.8.5/*/mixin-0.8.5-processor.jar`
3. ForgeGradle 下载的 mixin 包里也带 processor（同一个 class 集合）。

## 它是干嘛的

Mixin 的注解处理器会在编译期生成 `sevencurses.refmap.json`
（official 名 → SRG 名映射）。**没有它，reobf 之后的 jar 里 `@Inject` 会找不到方法**，
客户端启动时直接崩在 `InvalidInjectionException` / `@Shadow field not located`。
