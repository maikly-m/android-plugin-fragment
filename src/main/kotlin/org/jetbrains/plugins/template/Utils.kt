package org.jetbrains.plugins.template

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls
import java.io.File

object Utils {
    // 获取项目的源代码目录 (比如 src/main/kotlin)
    fun getSourceDirectory(project: Project, filePath: @NonNls String, virtualFile: VirtualFile): String {
        val baseDir = project.basePath ?: return ""
        baseDir.split(File.separatorChar).let {
            it[it.lastIndex].run {
                val removeSuffix = baseDir.removeSuffix(this)
                val name = getModuleNameFromFile(project, virtualFile)
                if (filePath.contains("/src/main/kotlin/")) {
                    val srcDir = File(removeSuffix, "${name}/src/main/kotlin")
                    return srcDir.absolutePath
                } else {
                    val srcDir = File(removeSuffix, "${name}/src/main/java")
                    return srcDir.absolutePath
                }
            }
        }
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
        val baseDir = project.basePath ?: return null
        baseDir.split(File.separatorChar).let {
            it[it.lastIndex].run {
                val removeSuffix = baseDir.removeSuffix(this)
                val name = getModuleNameFromFile(project, virtualFile)
                val buildGradleFile = File(removeSuffix, "${name}/build.gradle")
                CustomFileLogger.getInstance().logInfo("buildGradleFile = ${buildGradleFile}")
                if (buildGradleFile.exists()) {
                    return LocalFileSystem.getInstance().findFileByPath(buildGradleFile.absolutePath)
                } else {
                    val buildGradleFileKts = File(removeSuffix, "${name}/build.gradle.kts")
                    CustomFileLogger.getInstance().logInfo("buildGradleFileKts = ${buildGradleFileKts}")
                    if (buildGradleFileKts.exists()){
                        CustomFileLogger.getInstance().logInfo("in buildGradleFileKts = ${buildGradleFileKts}")
                        return LocalFileSystem.getInstance().findFileByPath(buildGradleFileKts.absolutePath)
                    }
                }
            }
        }
        CustomFileLogger.getInstance().logInfo("findBuildGradleFile null")
        return null
    }

    // 从 build.gradle 文件中解析 applicationId
    fun getApplicationIdFromBuildGradle(buildGradleFile: VirtualFile): String? {
        val gradleFileText = String(buildGradleFile.contentsToByteArray())

        // 查找 applicationId
        val regex = """applicationId\s+"([^"]+)"""".toRegex()
        val matchResult = regex.find(gradleFileText)
        val get = matchResult?.groupValues?.get(1)
        if (get != null){
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
        module?.name?.replace(".","/")?.let {
            if (it.endsWith("/main")){
                return it.replace("main","")
            }
        }
        return module?.name?.replace(".","/")
    }

    private fun getModuleForFile(project: Project, file: VirtualFile): com.intellij.openapi.module.Module? {
        // 通过 ProjectFileIndex 获取文件所在的模块
        val fileIndex = ProjectFileIndex.getInstance(project)
        return fileIndex.getModuleForFile(file)
    }

    fun isValidString(input: String): Boolean {
        // 正则表达式: ^[a-zA-Z]+$ 表示字符串必须只包含英文字母，且至少有一个字符
        val regex = "^[a-zA-Z]+$".toRegex()
        return input.matches(regex)
    }
}