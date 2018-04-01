package io.github.lizhangqu.plugin.compat

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.util.GFileUtils
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
        project.ext.providedAarCompat = this.&providedAarCompat

        project.ext.getPublishApFileCompat = this.&getPublishApFileCompat
        project.ext.getRDirCompat = this.&getRDirCompat
        project.ext.getRFileCompat = this.&getRFileCompat
        project.ext.getPackageForRCompatCompat = this.&getPackageForRCompatCompat
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
     * 导出aapt2 jni 和 aapt2 daemon mode 是否被废弃
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    boolean isAapt2JniAndAapt2DaemonModeDeprecated() {
        try {
            def aapt2JniEnumValue = resolveEnumValue("ENABLE_IN_PROCESS_AAPT2", Class.forName("com.android.build.gradle.options.DeprecatedOptions"))
            def aapt2DaemonModeEnumValue = resolveEnumValue("ENABLE_DAEMON_MODE_AAPT2", Class.forName("com.android.build.gradle.options.DeprecatedOptions"))
            return aapt2JniEnumValue != null && aapt2DaemonModeEnumValue != null
        } catch (Exception e) {

        }
        return false
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
                if (isAapt2JniAndAapt2DaemonModeDeprecated()) {
                    aapt2JniEnabled = false
                }
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
                if (isAapt2JniAndAapt2DaemonModeDeprecated()) {
                    aapt2DaemonEnabled = false
                }
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

    /**
     * 在application 插件中开启providedAar功能
     */
    @SuppressWarnings("UnnecessaryQualifiedReference")
    void providedAarCompat() {
        if (!project.getPlugins().hasPlugin("com.android.application")) {
            return
        }
        if (project.getConfigurations().findByName("providedAar") != null) {
            return
        }
        Configuration providedConfiguration = project.getConfigurations().findByName("provided")
        Configuration compileOnlyConfiguration = project.getConfigurations().findByName("compileOnly")
        if (providedConfiguration == null && compileOnlyConfiguration == null) {
            return
        }
        Configuration providedAarConfiguration = project.getConfigurations().create("providedAar")
        String androidGradlePluginVersion = getAndroidGradlePluginVersionCompat()
        if (androidGradlePluginVersion.startsWith("1.3") || androidGradlePluginVersion.startsWith("1.5") || androidGradlePluginVersion.startsWith("2.") || androidGradlePluginVersion.startsWith("3.")) {
            //大于等于1.3.0的版本让provided继承providedAar，低于1.3.0的版本，手动提取aar中的jar添加依赖
            if (compileOnlyConfiguration != null) {
                compileOnlyConfiguration.extendsFrom(providedAarConfiguration)
            } else {
                providedConfiguration.extendsFrom(providedAarConfiguration)
            }
        }
        if (androidGradlePluginVersion.startsWith("0.") || androidGradlePluginVersion.startsWith("1.0") || androidGradlePluginVersion.startsWith("1.1") || androidGradlePluginVersion.startsWith("1.2")) {
            //不支持小于1.3.0的版本，不含1.3.0
            throw new GradleException("Not support version ${androidGradlePluginVersion}, android gradle plugin must >=1.3.0")
        } else {
            def android = project.getExtensions().getByName("android")
            android.applicationVariants.all { def variant ->
                if (androidGradlePluginVersion.startsWith("1.3") || androidGradlePluginVersion.startsWith("1.5") || androidGradlePluginVersion.startsWith("2.0") || androidGradlePluginVersion.startsWith("2.1") || androidGradlePluginVersion.startsWith("2.2") || androidGradlePluginVersion.startsWith("2.3") || androidGradlePluginVersion.startsWith("2.4")) {
                    //支持1.3.0+ ~ 2.4.0+，且低于2.5.0，支持传递依赖
                    def prepareDependenciesTask = project.tasks.findByName("prepare${variant.getName().capitalize()}Dependencies")
                    if (prepareDependenciesTask) {
                        def removeSyncIssues = {
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
                                                    project.logger.info "[providedAar] WARNING: providedAar has been enabled in com.android.application you can ignore ${syncIssue}"
                                                    syncIssuesIterator.remove()
                                                }
                                            }
                                        }

                                        //兼容1.3.0~2.1.3版本，为了将provided的aar不参与打包，将isOptional设为true
                                        if (androidGradlePluginVersion.startsWith("1.3") || androidGradlePluginVersion.startsWith("1.5") || androidGradlePluginVersion.startsWith("2.0") || androidGradlePluginVersion.startsWith("2.1")) {
                                            def configurationDependencies = dependencyChecker.configurationDependencies
                                            List libraries = configurationDependencies.libraries
                                            libraries.each { library ->
                                                providedAarConfiguration.dependencies.each { providedDependency ->
                                                    String libName = library.getName()
                                                    if (libName.contains(providedDependency.group) && libName.contains(providedDependency.name) && libName.contains(providedDependency.version)) {
                                                        Field isOptionalField = library.getClass().getDeclaredField("isOptional")
                                                        Field modifiersField = Field.class.getDeclaredField("modifiers")
                                                        modifiersField.setAccessible(true)
                                                        modifiersField.setInt(isOptionalField, isOptionalField.getModifiers() & ~java.lang.reflect.Modifier.FINAL)
                                                        isOptionalField.setAccessible(true)
                                                        isOptionalField.setBoolean(library, true)
                                                        //为了递归调用可以引用，先声明再赋值
                                                        def fixDependencies = null
                                                        fixDependencies = { dependencies ->
                                                            dependencies.each { dependency ->
                                                                if (dependency.getClass() == library.getClass()) {
                                                                    isOptionalField.setBoolean(dependency, true)
                                                                    fixDependencies(dependency.dependencies)
                                                                }
                                                            }
                                                        }
                                                        fixDependencies(library.dependencies)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace()
                            }
                        }
                        if (androidGradlePluginVersion.startsWith("1.3") || androidGradlePluginVersion.startsWith("1.5") || androidGradlePluginVersion.startsWith("2.0") || androidGradlePluginVersion.startsWith("2.1")) {
                            //这里不处理as sync的时候会出错
                            def appPlugin = project.getPlugins().findPlugin("com.android.application")
                            def taskManager = appPlugin.getMetaClass().getProperty(appPlugin, "taskManager")
                            def dependencyManager = taskManager.getClass().getSuperclass().getMetaClass().getProperty(taskManager, "dependencyManager")
                            def extraModelInfo = dependencyManager.getMetaClass().getProperty(dependencyManager, "extraModelInfo")
                            Map<?, ?> syncIssues = extraModelInfo.getSyncIssues()
                            syncIssues.iterator().with { syncIssuesIterator ->
                                syncIssuesIterator.each { syncIssuePair ->
                                    if (syncIssuePair.getValue().getType() == 7 && syncIssuePair.getValue().getSeverity() == 2) {
                                        syncIssuesIterator.remove()
                                    }
                                }
                            }
                            //下面同2.2.0+处理
                            prepareDependenciesTask.configure removeSyncIssues
                        } else if (androidGradlePluginVersion.startsWith("2.2") || androidGradlePluginVersion.startsWith("2.3")) {
                            prepareDependenciesTask.configure removeSyncIssues
                        } else if (androidGradlePluginVersion.startsWith("2.4")) {
                            prepareDependenciesTask.doFirst removeSyncIssues
                        }
                    }
                } else if (androidGradlePluginVersion.startsWith("2.5") || androidGradlePluginVersion.startsWith("3.")) {
                    //支持2.5.0+ ~ 3.2.0+，支持传递依赖
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
                                def compileManifests = null
                                def runtimeManifests = null
                                Class appPreBuildTaskClass = Class.forName("com.android.build.gradle.internal.tasks.AppPreBuildTask")
                                try {
                                    //3.0.0+
                                    Field compileManifestsField = appPreBuildTaskClass.getDeclaredField("compileManifests")
                                    Field runtimeManifestsField = appPreBuildTaskClass.getDeclaredField("runtimeManifests")
                                    compileManifestsField.setAccessible(true)
                                    runtimeManifestsField.setAccessible(true)
                                    compileManifests = compileManifestsField.get(prepareBuildTask)
                                    runtimeManifests = runtimeManifestsField.get(prepareBuildTask)
                                } catch (Exception e) {
                                    try {
                                        //2.5.0+
                                        Field variantScopeField = appPreBuildTaskClass.getDeclaredField("variantScope")
                                        variantScopeField.setAccessible(true)
                                        def variantScope = variantScopeField.get(prepareBuildTask)
                                        //noinspection UnnecessaryQualifiedReference
                                        compileManifests = variantScope.getArtifactCollection(com.android.build.gradle.internal.publishing.AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH, com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactScope.ALL, com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.MANIFEST)
                                        runtimeManifests = variantScope.getArtifactCollection(com.android.build.gradle.internal.publishing.AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH, com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactScope.ALL, com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.MANIFEST)
                                    } catch (Exception e1) {
                                    }
                                }
                                try {
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
                                            project.getLogger()
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
                                                project.logger.info(
                                                        "[providedAar] WARNING: providedAar has been enabled in com.android.application you can ignore 'Android dependency '"
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


        //redirect warning log to info log
        def listenerBackedLoggerContext = project.getLogger().getMetaClass().getProperty(project.getLogger(), "context")
        def originalOutputEventListener = listenerBackedLoggerContext.getOutputEventListener()
        def originalOutputEventLevel = listenerBackedLoggerContext.getLevel()
        listenerBackedLoggerContext.setOutputEventListener({ def outputEvent ->
            def logLevel = originalOutputEventLevel.name()
            if (!("QUIET".equalsIgnoreCase(logLevel) || "ERROR".equalsIgnoreCase(logLevel))) {
                if ("WARN".equalsIgnoreCase(outputEvent.getLogLevel().name())) {
                    String message = outputEvent.getMessage()
                    //Provided dependencies can only be jars.
                    //provided dependencies can only be jars.
                    if (message != null && (message.contains("Provided dependencies can only be jars.") || message.contains("provided dependencies can only be jars. "))) {
                        project.logger.info(message)
                        return
                    }
                }
            }
            if (originalOutputEventListener != null) {
                originalOutputEventListener.onOutput(outputEvent)
            }
        })
    }

    /**
     * R.java包名兼容获取
     */
    String packageForRCompat(def processAndroidResourceTask) {
        if (processAndroidResourceTask == null) {
            return null
        }
        String packageForR = null
        try {
            packageForR = processAndroidResourceTask.getPackageForR()
        } catch (Exception e) {
            project.logger.info(e.getMessage())
        }
        if (packageForR == null) {
            try {
                packageForR = processAndroidResourceTask.getOriginalApplicationId()
            } catch (Exception e) {
                project.logger.info(e.getMessage())
            }
        }
        return packageForR
    }
    /**
     * R.java输出路径兼容获取
     */
    File rFileDirCompat(def processAndroidResourceTask) {
        if (processAndroidResourceTask == null) {
            return null
        }
        File rFileDir = processAndroidResourceTask.getSourceOutputDir()
        GFileUtils.mkdirs(rFileDir)
        return rFileDir
    }

    /**
     * resources.ap_输出路径兼容获取
     */
    File apFileCompat(def processAndroidResourceTask) {
        if (processAndroidResourceTask == null) {
            return null
        }
        File apFile = null
        try {
            apFile = processAndroidResourceTask.getPackageOutputFile()
        } catch (Exception e) {
            project.logger.info(e.getMessage())
        }

        String variantName = null
        if (apFile == null) {
            try {
                variantName = processAndroidResourceTask.getMetaClass().getMetaProperty("variantName").getProperty(processAndroidResourceTask)
                File resPackageOutputFolder = processAndroidResourceTask.getResPackageOutputFolder()
                apFile = new File(resPackageOutputFolder, "resources" + "-" + variantName + ".ap_");
            } catch (Exception e) {
                project.logger.info(e.getMessage())
            }
        }

        if (apFile == null) {
            apFile = project.file("build${File.separator}intermediates${File.separator}res${File.separator}resources" + "-" + variantName + ".ap_")
        }
        return apFile
    }

    /**
     * 暴露给外界获取publish ap的函数
     */
    File getPublishApFileCompat(String variantName) {
        if (variantName == null || variantName.length() == 0) {
            throw new GradleException("variantName 不能为空，且必须是驼峰形式")
        }
        def processAndroidResourceTask = project.tasks.findByName("process${variantName.capitalize()}Resources")
        File originalApFile = apFileCompat(processAndroidResourceTask)
        if (originalApFile == null) {
            String androidGradlePluginVersion = getAndroidGradlePluginVersionCompat()
            if (androidGradlePluginVersion.startsWith("0.") || androidGradlePluginVersion.startsWith("1.") || androidGradlePluginVersion.startsWith("2.")) {
                //驼峰转换为-分隔
                int length = variantName.length()
                StringBuilder sb = new StringBuilder(length)
                for (int i = 0; i < length; i++) {
                    char c = variantName.charAt(i);
                    if (Character.isUpperCase(c)) {
                        sb.append("-")
                        sb.append(Character.toLowerCase(c))
                    } else {
                        sb.append(c)
                    }
                }
                return project.file("build${File.separator}intermediates${File.separator}res${File.separator}resources-${sb}.ap_")
            } else {
                //驼峰转换为对应的目录和文件名
                int length = variantName.length()
                StringBuilder dirName = new StringBuilder(length)
                for (int i = 0; i < length; i++) {
                    char c = variantName.charAt(i);
                    if (Character.isUpperCase(c)) {
                        dirName.append(File.separator)
                        dirName.append(Character.toLowerCase(c))
                    } else {
                        dirName.append(c)
                    }
                }
                return project.file("build${File.separator}intermediates${File.separator}res${File.separator}${dirName}${File.separator}resources-${variantName}.ap_")
            }
        }
        return originalApFile
    }

    /**
     * 暴露给外界获取R.java除去包名路径的函数，必须在project.afterEvaluate中调用，否则获取到的是null
     */
    File getRDirCompat(String variantName) {
        if (variantName == null || variantName.length() == 0) {
            throw new GradleException("variantName 不能为空，且必须是驼峰形式")
        }
        def processAndroidResourceTask = project.tasks.findByName("process${variantName.capitalize()}Resources")
        File rDir = rFileDirCompat(processAndroidResourceTask)
        if (rDir == null) {
            int length = variantName.length()
            StringBuilder dirName = new StringBuilder(length)
            for (int i = 0; i < length; i++) {
                char c = variantName.charAt(i);
                if (Character.isUpperCase(c)) {
                    dirName.append(File.separator)
                    dirName.append(Character.toLowerCase(c))
                } else {
                    dirName.append(c)
                }
            }
            return project.file("build${File.separator}generated${File.separator}source${File.separator}r${File.separator}${dirName}")
        }
        return rDir
    }

    /**
     * 暴露给外界获取R.java路径的函数，必须在project.afterEvaluate中调用，否则获取到的是null
     */
    File getRFileCompat(String variantName) {
        if (variantName == null || variantName.length() == 0) {
            throw new GradleException("variantName 不能为空，且必须是驼峰形式")
        }
        File rDir = getRDirCompat(variantName)
        String packageForR = getPackageForRCompatCompat(variantName)
        return new File(new File(rDir, packageForR.replaceAll("\\.", File.separator)), "R.java")
    }

    /**
     * 暴露给外界获取R.java包名的函数，必须在project.afterEvaluate中调用，否则获取到的是null
     */
    String getPackageForRCompatCompat(String variantName) {
        if (variantName == null || variantName.length() == 0) {
            throw new GradleException("variantName 不能为空，且必须是驼峰形式")
        }
        def processAndroidResourceTask = project.tasks.findByName("process${variantName.capitalize()}Resources")
        String packageForR = packageForRCompat(processAndroidResourceTask)
        if (packageForR == null) {
            return project.android.defaultConfig.applicationId
        }
        return packageForR
    }

}
