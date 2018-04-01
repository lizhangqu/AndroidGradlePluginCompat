# Android Gradle Plugin兼容插件


### 在com.android.application中使用provided aar功能

首先调用一遍

```
providedAarCompat()
```

然后就可以正常使用providedAar了，如

```
dependencies {
    providedAar 'com.tencent.tinker:tinker-android-lib:1.9.1'
}
```

目前只支持如下版本
 - android gradle plugin [1.3.0,3.1.0], 支持传递依赖 
 
小于1.3.0的版本不支持，抛异常

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

### 获取R.java文件中的包名

```
String packageForR = getPackageForRCompatCompat('release')
```

### 获取R.java文件路径

```
File rFile = getRFileCompat('release')
```


### 获取R.java文件去除包名后的父路径

```
File rDir = getRDirCompat('release')
```

### 获取resource.ap_文件路径

```
File apFile = getPublishApFileCompat('release')
```