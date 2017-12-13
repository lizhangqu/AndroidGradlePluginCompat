# Android Gradle Plugin兼容插件


### aapt2是否开启

```
if (isAapt2EnabledCompat()) {

} else {

}
```

### aapt2 jni方式是否开启

```
if (isAapt2JniEnabledCompat()) {

} else {

}
```

### aapt2 守护进程方式是否开启

```
if (isAapt2DaemonModeEnabledCompat()) {

} else {

}
```

### 获取 android gradle plugin 版本号

```
String androidGradlePluginVersion = getAndroidGradlePluginVersionCompat()
```