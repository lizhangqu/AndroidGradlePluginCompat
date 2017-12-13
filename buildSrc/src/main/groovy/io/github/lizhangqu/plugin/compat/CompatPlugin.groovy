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
}

