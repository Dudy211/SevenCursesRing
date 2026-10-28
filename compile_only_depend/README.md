# compile_only_depend

这里需要放一个**第三方 mod jar**（不随仓库分发）：

```
compile_only_depend/curios-forge-5.14.1+1.20.1.jar
```

`build.gradle` 里这样用它：

```groovy
compileOnly fileTree(dir: 'compile_only_depend', includes: ['*.jar'])
```

**只是编译期依赖** —— 运行期由玩家自己安装的 Curios API 提供（`mods.toml` 里已声明 `curios` 依赖）。

## 怎么拿到

- CurseForge / Modrinth 下载 Curios API 的 Forge 1.20.1 版本（5.14.1+ 均可），
  把 jar 原名丢进这个目录即可（`*.jar` 通配）。
- 或用作者 maven：`https://maven.theillusivec4.top/`
  （`top.theillusivec4.curios:curios-forge:5.14.1+1.20.1`）

## 为什么用本地 jar 而不是 maven 坐标

这个项目在离线环境开发，作者 maven 未接入；换成 maven 坐标也是可以的，
只是要改 `build.gradle` 的 `repositories` 与 `dependencies` 两处。
