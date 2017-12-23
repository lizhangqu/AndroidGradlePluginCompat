package io.github.lizhangqu.plugin.compat

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult

import java.lang.reflect.Field

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
        project.ext.isAapt2DaemonModeEnabledCompat = this.&isAapt2DaemonModeEnabledCompat
        project.ext.getAndroidGradlePluginVersionCompat = this.&getAndroidGradlePluginVersionCompat
        project.ext.isJenkins = this.&isJenkins
        project.ext.providedCompat = this.&providedCompat
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

    /**
     * 导出aapt2DaemonMode是否开启的兼容方法，build.gradle中apply后可直接使用isAapt2DaemonModeEnabledCompat()
     */
    boolean isAapt2DaemonModeEnabledCompat() {
        boolean aapt2DaemonEnabled = false
        if (isAapt2EnabledCompat()) {
            try {
                def projectOptions = getProjectOptions()
                def enumValue = resolveEnumValue("ENABLE_DAEMON_MODE_AAPT2", Class.forName("com.android.build.gradle.options.BooleanOption"))
                aapt2DaemonEnabled = projectOptions.get(enumValue)
            } catch (Exception e) {
                aapt2DaemonEnabled = false
            }
        }
        return aapt2DaemonEnabled
    }

    /**
     * 导出获得android gradle plugin插件的版本号，build.gradle中apply后可直接使用getAndroidGradlePluginVersionCompat()
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    String getAndroidGradlePluginVersionCompat() {
        String version = null
        try {
            Class versionModel = Class.forName("com.android.builder.model.Version")
            def versionFiled = versionModel.getDeclaredField("ANDROID_GRADLE_PLUGIN_VERSION")
            versionFiled.setAccessible(true)
            version = versionFiled.get(null)
        } catch (Exception e) {
            version = "unknown"
        }
        return version
    }

    /**
     * 导出是否在jenkins环境中,build.gradle中apply后可直接使用isJenkins()
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    boolean isJenkins() {
        Map<String, String> environmentMap = System.getenv()
        boolean result = false
        if (environmentMap != null && environmentMap.containsKey("JOB_NAME") && environmentMap.containsKey("BUILD_NUMBER")) {
            result = true
        }
        return result
    }

    boolean providedCompatRun = false

    void providedCompat() {
        if (providedCompatRun) {
            return
        }
        providedCompatRun = true
        if (!project.getPlugins().hasPlugin("com.android.application")) {
            return
        }
        Configuration providedConfiguration = project.getConfigurations().findByName("provided")
        if (providedConfiguration == null) {
            return
        }
        providedConfiguration.extendsFrom(project.getConfigurations().create("providedAar"))
        String androidGradlePluginVersion = getAndroidGradlePluginVersionCompat()
        def android = project.getExtensions().getByName("android")
        android.applicationVariants.all { def variant ->
            if (androidGradlePluginVersion.startsWith("1.")) {
                //不支持
            } else if (androidGradlePluginVersion.startsWith("2.0") || androidGradlePluginVersion.startsWith("2.1")) {
                //不支持
            } else if (androidGradlePluginVersion.startsWith("2.2") || androidGradlePluginVersion.startsWith("2.3")) {
                //支持2.2.3和2.3.3
                def prepareDependenciesTask = project.tasks.findByName("prepare${variant.getName().capitalize()}Dependencies")
                if (prepareDependenciesTask) {
                    prepareDependenciesTask.configure {
                        try {
                            Class prepareDependenciesTaskClass = Class.forName("com.android.build.gradle.internal.tasks.PrepareDependenciesTask")
                            Field checkersField = prepareDependenciesTaskClass.getDeclaredField('checkers')
                            checkersField.setAccessible(true)
                            def checkers = checkersField.get(prepareDependenciesTask)
                            checkers.iterator().with { checkersIterator ->
                                checkersIterator.each { dependencyChecker ->
                                    def syncIssues = dependencyChecker.syncIssues
                                    syncIssues.iterator().with { syncIssuesIterator ->
                                        syncIssuesIterator.each { syncIssue ->
                                            if (syncIssue.getType() == 7 && syncIssue.getSeverity() == 2) {
                                                project.logger.lifecycle "WARNING: providedAar has been enabled in com.android.application you can ignore ${syncIssue}"
                                                syncIssuesIterator.remove()
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace()
                        }
                    }
                }
            } else if (androidGradlePluginVersion.startsWith("3.")) {
                //支持3.*
                def prepareBuildTask = project.tasks.findByName("pre${variant.getName().capitalize()}Build")
                if (prepareBuildTask) {
                    boolean needRedirectAction = false
                    prepareBuildTask.actions.iterator().with { actionsIterator ->
                        actionsIterator.each { action ->
                            if (action.getActionClassName().contains("AppPreBuildTask")) {
                                actionsIterator.remove()
                                needRedirectAction = true
                            }
                        }
                    }
                    if (needRedirectAction) {
                        prepareBuildTask.doLast {
                            try {
                                Class appPreBuildTaskClass = Class.forName("com.android.build.gradle.internal.tasks.AppPreBuildTask")
                                Field compileManifestsField = appPreBuildTaskClass.getDeclaredField("compileManifests")
                                Field runtimeManifestsField = appPreBuildTaskClass.getDeclaredField("runtimeManifests")
                                compileManifestsField.setAccessible(true)
                                runtimeManifestsField.setAccessible(true)
                                def compileManifests = compileManifestsField.get(prepareBuildTask)
                                def runtimeManifests = runtimeManifestsField.get(prepareBuildTask)
                                Set<ResolvedArtifactResult> compileArtifacts = compileManifests.getArtifacts()
                                Set<ResolvedArtifactResult> runtimeArtifacts = runtimeManifests.getArtifacts()

                                Map<String, String> runtimeIds = new HashMap<>(runtimeArtifacts.size())

                                def handleArtifact = { id, consumer ->
                                    if (id instanceof ProjectComponentIdentifier) {
                                        consumer(((ProjectComponentIdentifier) id).getProjectPath().intern(), "")
                                    } else if (id instanceof ModuleComponentIdentifier) {
                                        ModuleComponentIdentifier moduleComponentId = (ModuleComponentIdentifier) id
                                        consumer(
                                                moduleComponentId.getGroup() + ":" + moduleComponentId.getModule(),
                                                moduleComponentId.getVersion())
                                    } else {
                                        getLogger()
                                                .warn(
                                                "Unknown ComponentIdentifier type: "
                                                        + id.getClass().getCanonicalName())
                                    }
                                }

                                runtimeArtifacts.each { def artifact ->
                                    def runtimeId = artifact.getId().getComponentIdentifier()
                                    def putMap = { def key, def value ->
                                        runtimeIds.put(key, value)
                                    }
                                    handleArtifact(runtimeId, putMap)
                                }

                                compileArtifacts.each { def artifact ->
                                    final ComponentIdentifier compileId = artifact.getId().getComponentIdentifier()
                                    def checkCompile = { def key, def value ->
                                        String runtimeVersion = runtimeIds.get(key)
                                        if (runtimeVersion == null) {
                                            String display = compileId.getDisplayName()
                                            project.logger.lifecycle(
                                                    "WARNING: providedAar has been enabled in com.android.application you can ignore 'Android dependency '"
                                                            + display
                                                            + "' is set to compileOnly/provided which is not supported'")
                                        } else if (!runtimeVersion.isEmpty()) {
                                            // compare versions.
                                            if (!runtimeVersion.equals(value)) {
                                                throw new RuntimeException(
                                                        String.format(
                                                                "Android dependency '%s' has different version for the compile (%s) and runtime (%s) classpath. You should manually set the same version via DependencyResolution",
                                                                key, value, runtimeVersion));
                                            }
                                        }
                                    }
                                    handleArtifact(compileId, checkCompile)
                                }
                            } catch (Exception e) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}
