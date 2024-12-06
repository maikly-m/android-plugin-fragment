package org.jetbrains.plugins.template

import com.intellij.ide.startup.importSettings.providers.vswin.utilities.VSHive.Companion.regex
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile
import org.jetbrains.annotations.NonNls
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

object Utils {
    // 获取项目的源代码目录 (比如 src/main/kotlin)
    fun getSourceDirectory(
        project: Project,
        filePath: @NonNls String,
        virtualFile: VirtualFile
    ): String? {
        val baseDir = project.basePath ?: return ""
        getModuleNameFromFile(project, virtualFile)?.run {
            var relativePath = ""
            if (contains(File.separatorChar)) {
                substringAfter(File.separatorChar).let {
                    // 移除第一个位置的project.name
                    // ModuleName可能是abc/a ，abc是setting.gradle下的项目名字；
                    // 但是项目在系统的文文件位置可能是xxx/dfg,dfg是项目的根位置
                    relativePath = "$it/"
                }
            } else {
                relativePath = ""
            }
            if (filePath.contains("/src/main/kotlin/")) {
                val srcDir = File(baseDir, "${relativePath}src/main/kotlin")
                return srcDir.absolutePath
            } else {
                val srcDir = File(baseDir, "${relativePath}src/main/java")
                return srcDir.absolutePath
            }
        }
        return null
    }

    // 根据文件路径推断包名
    fun getPackageNameFromFilePath(filePath: String, sourceDirectory: String): String {
        // 确保文件在 src/main/kotlin 目录下
        val relativePath = filePath.removePrefix(sourceDirectory)
        val packagePath = relativePath.replace(File.separatorChar, '.')
        return packagePath.removePrefix(".")  // 去除开头的点（如果有）
    }


    // 查找模块的 build.gradle 文件
    fun findBuildGradleFile(project: Project, virtualFile: VirtualFile): VirtualFile? {

        // 获取根项目名称
        val rootProjectName = project.name
        CustomFileLogger.getInstance().logInfo("Root project name: $rootProjectName")
        // 获取根项目路径
        val rootProjectPath = project.basePath
        CustomFileLogger.getInstance().logInfo("Root project path: $rootProjectPath")
        CustomFileLogger.getInstance().logInfo("virtualFile path: ${virtualFile.path}")
        CustomFileLogger.getInstance()
            .logInfo("virtualFile project.baseDir: ${project.baseDir.path}")

        val baseDir = project.basePath ?: return null
        getModuleNameFromFile(project, virtualFile)?.run {
            var relativePath = ""
            if (contains(File.separatorChar)) {
                substringAfter(File.separatorChar).let {
                    // 移除第一个位置的project.name
                    // ModuleName可能是abc/a ，abc是setting.gradle下的项目名字；
                    // 但是项目在系统的文文件位置可能是xxx/dfg,dfg是项目的根位置
                    relativePath = "$it/"
                }
            } else {
                relativePath = ""
            }
            val buildGradleFile = File(baseDir, "${relativePath}build.gradle")
            CustomFileLogger.getInstance().logInfo("buildGradleFile = ${buildGradleFile}")
            if (buildGradleFile.exists()) {
                return LocalFileSystem.getInstance().findFileByPath(buildGradleFile.absolutePath)
            } else {
                val buildGradleFileKts = File(baseDir, "${relativePath}build.gradle.kts")
                CustomFileLogger.getInstance().logInfo("buildGradleFileKts = ${buildGradleFileKts}")
                if (buildGradleFileKts.exists()) {
                    CustomFileLogger.getInstance()
                        .logInfo("in buildGradleFileKts = ${buildGradleFileKts}")
                    return LocalFileSystem.getInstance()
                        .findFileByPath(buildGradleFileKts.absolutePath)
                }
            }
        }
        CustomFileLogger.getInstance().logInfo("findBuildGradleFile null")
        return null
    }

    // 查找模块的 app 文件夹名字
    fun findAppFileRelativePath(project: Project, virtualFile: VirtualFile): String? {
        getModuleNameFromFile(project, virtualFile)?.run {
            var relativePath = ""
            if (contains(File.separatorChar)) {
                substringAfter(File.separatorChar).let {
                    // 移除第一个位置的project.name
                    // ModuleName可能是abc/a ，abc是setting.gradle下的项目名字；
                    // 但是项目在系统的文文件位置可能是xxx/dfg,dfg是项目的根位置
                    relativePath = "$it/"
                }
            } else {
                relativePath = ""
            }
            return relativePath
        }
        return null
    }

    // 从 build.gradle 文件中解析 applicationId
    fun getApplicationIdFromBuildGradle(buildGradleFile: VirtualFile): String? {
        val gradleFileText = String(buildGradleFile.contentsToByteArray())

        // 查找 applicationId
        val regex = """applicationId\s+"([^"]+)"""".toRegex()
        val matchResult = regex.find(gradleFileText)
        val get = matchResult?.groupValues?.get(1)
        if (get != null) {
            return get
        }
        // 正则表达式匹配 applicationId
        val regex2 = """applicationId\s*=\s*["'](.*?)["']""".toRegex()
        val matchResult2 = regex2.find(gradleFileText)
        return matchResult2?.groupValues?.get(1)
    }

    fun getModuleNameFromEditor(project: Project): String? {
        // 获取当前活动的编辑器
        val editor: Editor? = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            val virtualFile: VirtualFile? = editor.virtualFile
            if (virtualFile != null) {
                return getModuleNameFromFile(project, virtualFile)
            }
        }
        return null
    }

    fun getModuleNameFromFile(project: Project, virtualFile: VirtualFile): String? {
        // 通过文件路径推断所属模块
        val module = ModuleUtil.findModuleForFile(virtualFile, project)
        CustomFileLogger.getInstance().logInfo("module = ${module?.moduleFilePath}")
        module?.name?.replace(".", "/")?.let {
            if (it.endsWith("/main")) {
                return it.replace("main", "")
            }
        }
        return module?.name?.replace(".", "/")
    }

    private fun getModuleForFile(
        project: Project,
        file: VirtualFile
    ): com.intellij.openapi.module.Module? {
        // 通过 ProjectFileIndex 获取文件所在的模块
        val fileIndex = ProjectFileIndex.getInstance(project)
        return fileIndex.getModuleForFile(file)
    }

    fun isValidString(input: String): Boolean {
        // 正则表达式: ^[a-zA-Z]+$ 表示字符串必须只包含英文字母，且至少有一个字符
        val regex = "^[a-zA-Z]+$".toRegex()
        return input.matches(regex)
    }

    fun getPackageNameFromManifest(project: Project, virtualFile: VirtualFile): String? {
        val baseDir = project.basePath ?: return null
        getModuleNameFromFile(project, virtualFile)?.run {
            var relativePath = ""
            if (contains(File.separatorChar)) {
                substringAfter(File.separatorChar).let {
                    // 移除第一个位置的project.name
                    // ModuleName可能是abc/a ，abc是setting.gradle下的项目名字；
                    // 但是项目在系统的文文件位置可能是xxx/dfg,dfg是项目的根位置
                    relativePath = "$it/"
                }
            } else {
                relativePath = ""
            }
            val manifestFile = File(baseDir, "${relativePath}src/main/AndroidManifest.xml")
            if (!manifestFile.exists()) {
                return null
            }
            LocalFileSystem.getInstance().findFileByPath(manifestFile.absolutePath)?.let {
                val psiFile = it.let {
                    PsiFileFactory.getInstance(project)
                        .createFileFromText("AndroidManifest.xml", inputStreamToString(it.inputStream))
                }
                CustomFileLogger.getInstance()
                    .logInfo("getPackageNameFromManifest virtualFile.name=${it.name}")
                CustomFileLogger.getInstance()
                    .logInfo("getPackageNameFromManifest virtualFile.path=${it.path}")
//                CustomFileLogger.getInstance()
//                    .logInfo("getPackageNameFromManifest virtualFile.text=${psiFile.text}")

                // 解析 XML 文件，查找 <manifest> 元素中的 package 属性
                val xmlFile = psiFile as? XmlFile ?: return null
                val manifestTag = xmlFile.document?.rootTag ?: return null
                return manifestTag.getAttributeValue("package")
            }

        }
        return null
    }

    fun getNamespaceFromGradle(project: Project, file: VirtualFile): String? {
        findBuildGradleFile(project, file)?.let {
            // 创建 PsiFile 来解析 Gradle 文件内容
            val psiFile: PsiFile = PsiFileFactory.getInstance(project)
                .createFileFromText(it.name,  inputStreamToString(it.inputStream))
            CustomFileLogger.getInstance()
                .logInfo("getNamespaceFromGradle virtualFile.name=${it.name}")
            CustomFileLogger.getInstance()
                .logInfo("getNamespaceFromGradle virtualFile.path=${it.path}")
            // 获取文件内容并查找 namespace 配置项
            val content = psiFile.text ?: return null
            // 使用正则表达式解析 namespace
            val regex : Regex?
            if (it.name == "build.gradle") {
                //gradle
                regex = "namespace\\s*\"([^\"]+)\"".toRegex()
            } else {
                // kts
                regex = "namespace\\s*=\\s*\"([^\"]+)\"".toRegex()
            }
            val r = regex.find(content)?.groups?.get(1)?.value
            CustomFileLogger.getInstance()
                .logInfo("getPackageNameFromManifest result=${r}")
            return r
        }
        return null
    }
    private fun inputStreamToString(inputStream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.use { it.readText() }
    }
}
