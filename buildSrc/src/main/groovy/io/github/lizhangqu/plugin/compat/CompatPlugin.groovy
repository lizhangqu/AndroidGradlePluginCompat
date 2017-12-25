package io.github.lizhangqu.plugin.compat

import org.gradle.StartParameter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceArtifactResolver
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceResolver
import org.gradle.api.internal.artifacts.repositories.resolver.MavenResolver
import org.gradle.internal.component.external.model.DefaultModuleComponentArtifactIdentifier
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier
import org.gradle.internal.resolve.result.DefaultResourceAwareResolveResult
import org.gradle.internal.resource.local.LazyLocallyAvailableResourceCandidates
import org.gradle.internal.resource.local.LocallyAvailableExternalResource
import org.gradle.internal.resource.local.LocallyAvailableResource
import org.gradle.internal.resource.local.LocallyAvailableResourceCandidates
import org.gradle.internal.resource.local.LocallyAvailableResourceFinder

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

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
        project.ext.providedAarCompat = this.&providedAarCompat
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

    void providedAarCompat() {
        if (!project.getPlugins().hasPlugin("com.android.application")) {
            return
        }
        if (project.getConfigurations().findByName("providedAar") != null) {
            return
        }
        Configuration providedConfiguration = project.getConfigurations().findByName("provided")
        if (providedConfiguration == null) {
            return
        }
        Configuration providedAarConfiguration = project.getConfigurations().create("providedAar")
        String androidGradlePluginVersion = getAndroidGradlePluginVersionCompat()
        if (androidGradlePluginVersion.startsWith("2.2") || androidGradlePluginVersion.startsWith("2.3") || androidGradlePluginVersion.startsWith("3.")) {

        }
        if (androidGradlePluginVersion.startsWith("1.") || androidGradlePluginVersion.startsWith("2.0") || androidGradlePluginVersion.startsWith("2.1")) {
            /**
             * 获取是否是离线模式
             */
            StartParameter startParameter = project.gradle.startParameter
            boolean isOffline = startParameter.isOffline()
            project.logger.error("[providedAar] gradle offline: ${isOffline}")
            if (isOffline) {
                project.logger.error("[providedAar] use local cache dependency because offline is enabled")
            } else {
                project.logger.error("[providedAar] use remote dependency because offline is disabled")
            }
            /**
             * 创建ModuleComponentArtifactMetaData对象，之所以用反射，是因为gradle的不同版本，这个类的名字发生了变化，低版本可能是DefaultModuleComponentArtifactMetaData，而高版本改成了org.gradle.internal.component.external.model.DefaultModuleComponentArtifactMetadata
             */
            def createModuleComponentArtifactMetaData = { String group, String name, String version, String type, String extension ->
                try {
                    ModuleComponentIdentifier componentIdentifier = DefaultModuleComponentIdentifier.newId(group, name, version)
                    DefaultModuleComponentArtifactIdentifier moduleComponentArtifactIdentifier = new DefaultModuleComponentArtifactIdentifier(componentIdentifier, name, type, extension)
                    Class moduleComponentArtifactMetadataClass = null
                    try {
                        moduleComponentArtifactMetadataClass = Class.forName("org.gradle.internal.component.external.model.DefaultModuleComponentArtifactMetaData")
                    } catch (ClassNotFoundException e) {
                        try{
                            moduleComponentArtifactMetadataClass = Class.forName("org.gradle.internal.component.external.model.DefaultModuleComponentArtifactMetadata")
                        }catch (ClassNotFoundException e1){

                        }
                    }
                    if (moduleComponentArtifactMetadataClass == null) {
                        return null
                    }
                    Constructor moduleComponentArtifactMetadataConstructor = moduleComponentArtifactMetadataClass.getDeclaredConstructor(ModuleComponentArtifactIdentifier.class)
                    moduleComponentArtifactMetadataConstructor.setAccessible(true)
                    return moduleComponentArtifactMetadataConstructor.newInstance(moduleComponentArtifactIdentifier)
                } catch (Exception e) {
                    e.printStackTrace()
                }
                return null
            }

            /**
             * 创建ExternalResourceArtifactResolver对象，用反射的原因是这个方法是protected的
             */
            def createArtifactResolver = { MavenResolver mavenResolver ->
                if (mavenResolver != null) {
                    Method createArtifactResolverMethod = ExternalResourceResolver.class.getDeclaredMethod("createArtifactResolver")
                    createArtifactResolverMethod.setAccessible(true)
                    return createArtifactResolverMethod.invoke(mavenResolver)
                }
                return null
            }

            project.getGradle().addListener(new DependencyResolutionListener() {
                @Override
                void beforeResolve(ResolvableDependencies dependencies) {
                    //此回调会多次进入，我们只需要解析一次，因此只要进入，就remove，然后执行我们的解析操作
                    project.gradle.removeListener(this)
                    //遍历providedAar的所有依赖
                    providedAarConfiguration.getDependencies().each { def dependency ->
                        def moduleComponentArtifactMetadata = createModuleComponentArtifactMetaData(dependency.group, dependency.name, dependency.version, "aar", "aar")
                        //从repositories中去找，用any是因为只要找到一个就需要return
                        project.getRepositories().any { def repository ->
                            //只处理maven
                            if (repository instanceof DefaultMavenArtifactRepository) {
                                if (moduleComponentArtifactMetadata != null) {
                                    MavenResolver mavenResolver = repository.createResolver()
                                    ExternalResourceArtifactResolver externalResourceArtifactResolver = createArtifactResolver(mavenResolver)
                                    LocallyAvailableResourceFinder locallyAvailableResourceFinder = externalResourceArtifactResolver.locallyAvailableResourceFinder
                                    //离线模式，走本地缓存
                                    if (isOffline) {
                                        LocallyAvailableResourceCandidates locallyAvailableResourceCandidates = locallyAvailableResourceFinder.findCandidates(moduleComponentArtifactMetadata)
                                        //如果本地的候选列表不为空
                                        if (!locallyAvailableResourceCandidates.isNone()) {
                                            Class compositeLocallyAvailableResourceCandidatesClass = Class.forName('org.gradle.internal.resource.local.CompositeLocallyAvailableResourceFinder$CompositeLocallyAvailableResourceCandidates')
                                            if (compositeLocallyAvailableResourceCandidatesClass.isInstance(locallyAvailableResourceCandidates)) {
                                                //获取这个组合的候选列表并遍历它，取其中一个，然后return
                                                Field allCandidatesField = compositeLocallyAvailableResourceCandidatesClass.getDeclaredField("allCandidates")
                                                allCandidatesField.setAccessible(true)
                                                List<LocallyAvailableResourceCandidates> allCandidates = allCandidatesField.get(locallyAvailableResourceCandidates)
                                                if (allCandidates != null) {
                                                    FileCollection aarFiles = null
                                                    //用any的原因是为了取一个就返回
                                                    allCandidates.any { candidate ->
                                                        //判断是否是LazyLocallyAvailableResourceCandidates实例
                                                        if (candidate instanceof LazyLocallyAvailableResourceCandidates) {
                                                            //如果该候选列表存在文件，则获取文件，然后过滤aar文件，返回
                                                            if (!candidate.isNone()) {
                                                                Method getFilesMethod = LazyLocallyAvailableResourceCandidates.class.getDeclaredMethod("getFiles")
                                                                getFilesMethod.setAccessible(true)
                                                                List<File> candidateFiles = getFilesMethod.invoke(candidate)
                                                                aarFiles = project.files(candidateFiles).filter {
                                                                    it.name.endsWith(".aar")
                                                                }

                                                                if (!aarFiles.empty) {
                                                                    return true
                                                                }
                                                            }
                                                        }
                                                    }
                                                    //如果找到了aar文件，则提取jar，添加到provided的scope上
                                                    if (!aarFiles.empty) {
                                                        aarFiles.files.each { File aarFile ->
                                                            FileCollection jarFromAar = project.zipTree(aarFile).filter {
                                                                it.name == "classes.jar"
                                                            }
                                                            project.getDependencies().add("provided", jarFromAar)
                                                            project.logger.error("[providedAar] convert aar ${dependency.group}:${dependency.name}:${dependency.version} to jar and add provided file ${jarFromAar.getAsPath()} from ${aarFile}")
                                                        }
                                                        return true
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        //在线模式，走远程依赖，实际逻辑gradle内部处理
                                        boolean artifactExists = externalResourceArtifactResolver.artifactExists(moduleComponentArtifactMetadata, new DefaultResourceAwareResolveResult())
                                        //如果该远程仓库存在该依赖
                                        if (artifactExists) {
                                            //获取该依赖对应的文件，提取jar，添加到provided的scope上
                                            LocallyAvailableExternalResource locallyAvailableExternalResource = externalResourceArtifactResolver.resolveArtifact(moduleComponentArtifactMetadata, new DefaultResourceAwareResolveResult())
                                            if (locallyAvailableExternalResource != null) {
                                                File aarFile = null
                                                try {
                                                    def locallyAvailableResource = locallyAvailableExternalResource.getLocalResource()
                                                    if (locallyAvailableResource != null) {
                                                        aarFile = locallyAvailableResource.getFile()
                                                    }
                                                } catch (Exception e) {
                                                    //高版本gradle兼容
                                                    try {
                                                        aarFile = locallyAvailableExternalResource.getFile()
                                                    } catch (Exception e1) {

                                                    }
                                                }

                                                if (aarFile != null && aarFile.exists()) {
                                                    FileCollection jarFromAar = project.zipTree(aarFile).filter {
                                                        it.name == "classes.jar"
                                                    }
                                                    project.getDependencies().add("provided", jarFromAar)
                                                    project.logger.error("[providedAar] convert aar ${dependency.group}:${dependency.name}:${dependency.version} in ${repository.url} to jar and add provided file ${jarFromAar.getAsPath()} from ${aarFile}")
                                                    return true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                void afterResolve(ResolvableDependencies resolvableDependencies) {

                }
            })
        } else {
            def android = project.getExtensions().getByName("android")
            android.applicationVariants.all { def variant ->
                if (androidGradlePluginVersion.startsWith("1.") || androidGradlePluginVersion.startsWith("2.0") || androidGradlePluginVersion.startsWith("2.1")) {
                    //不在这里处理
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
}
