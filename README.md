# Android Gradle Plugin兼容插件


### 在com.android.application中使用provided aar功能

首先调用一遍
```
providedCompat()
```

然后就可以正常使用providedAar了，如

```
dependencies {
    providedAar 'com.tencent.tinker:tinker-android-lib:1.9.1'
}
```

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

### 是否在jenkins环境中

```
boolean onJenkins = isJenkins()
```