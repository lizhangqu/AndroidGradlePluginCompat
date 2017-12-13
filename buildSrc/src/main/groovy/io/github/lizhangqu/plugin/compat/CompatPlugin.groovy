package io.github.lizhangqu.plugin.compat

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Android Gradle Plugin兼容插件
 */
class CompatPlugin implements Plugin<Project> {
    Project project

    @Override
    void apply(Project project) {
        this.project = project
        project.ext.isAapt2EnabledCompat = this.&isAapt2EnabledCompat
        project.ext.isAapt2JniEnabledCompat = this.&isAapt2JniEnabledCompat
    }

    static <T> T resolveEnumValue(String value, Class<T> type) {
        for (T constant : type.getEnumConstants()) {
            if (constant.toString().equalsIgnoreCase(value)) {
                return constant
            }
        }
        return null
    }


    def getProjectOptions() {
        Class classProjectOptions = Class.forName("com.android.build.gradle.options.ProjectOptions")
        def constructor = classProjectOptions.getDeclaredConstructor(Project.class)
        constructor.setAccessible(true)
        def projectOptions = constructor.newInstance(project)
        return projectOptions
    }

    /**
     * 导出aapt2是否开启的兼容方法，build.gradle中apply后可直接使用isAapt2EnabledCompat()
     */
    boolean isAapt2EnabledCompat() {
        boolean aapt2Enabled = false
        try {
            def projectOptions = getProjectOptions()
            Object enumValue = resolveEnumValue("ENABLE_AAPT2", Class.forName("com.android.build.gradle.options.BooleanOption"))
            aapt2Enabled = projectOptions.get(enumValue)
        } catch (Exception e) {
            try {
                //gradle 2.3.3的方法
                Class classAndroidGradleOptions = Class.forName("com.android.build.gradle.AndroidGradleOptions")
                def isAapt2Enabled = classAndroidGradleOptions.getDeclaredMethod("isAapt2Enabled", Project.class)
                isAapt2Enabled.setAccessible(true)
                aapt2Enabled = isAapt2Enabled.invoke(null, project)
            } catch (Exception e1) {
                //都取不到表示还不支持
                aapt2Enabled = false
            }
        }
        return aapt2Enabled
    }

    /**
     * 导出aapt2Jni是否开启的兼容方法，build.gradle中apply后可直接使用isAapt2JniEnabledCompat()
     */
    boolean isAapt2JniEnabledCompat() {
        boolean aapt2JniEnabled = false
        if (isAapt2EnabledCompat()) {
            try {
                def projectOptions = getProjectOptions()
                def enumValue = resolveEnumValue("ENABLE_IN_PROCESS_AAPT2", Class.forName("com.android.build.gradle.options.BooleanOption"))
                aapt2JniEnabled = projectOptions.get(enumValue)
            } catch (Exception e) {
                aapt2JniEnabled = false
            }
        }
        return aapt2JniEnabled
    }
}

