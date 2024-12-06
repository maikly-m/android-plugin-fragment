package org.jetbrains.plugins.template.action

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.template.CustomFileLogger
import org.jetbrains.plugins.template.Utils.findBuildGradleFile
import org.jetbrains.plugins.template.Utils.getApplicationIdFromBuildGradle
import org.jetbrains.plugins.template.Utils.getPackageNameFromFilePath
import org.jetbrains.plugins.template.Utils.getSourceDirectory

class CreateViewModelFile {
    fun createFile(project: Project, folder: VirtualFile, name: String) {
        // 获取文件目录
        val psiDirectory = PsiManager.getInstance(project).findDirectory(folder) ?: return

        // 创建新 Kotlin 文件
        WriteCommandAction.runWriteCommandAction(project) {
            // 自动生成 Fragment 类代码
            val fragmentCode = generateCode(project, psiDirectory, folder, name)
            // 创建新文件
            val newFile = createKotlinFile(psiDirectory, "${name}ViewModel.kt", fragmentCode)
            newFile?.let {
                // 刷新文件，以便在 IDE 中看到新文件
                FileDocumentManager.getInstance().saveAllDocuments()
            }
        }
    }

    private fun generateCode(
        project: Project,
        psiDirectory: PsiDirectory,
        folder: VirtualFile,
        fileName: String
    ): String {
        // 自动生成一个简单的 Fragment 代码模板
        val nameWithoutSuffix = fileName.removeSuffix(".kt")
        var packageName = "com.example"

        // 获取当前文件夹的路径
        val filePath = folder.path
        CustomFileLogger.getInstance().logInfo("filePath = ${filePath}")
        getSourceDirectory(project, filePath, folder)?.let {
            CustomFileLogger.getInstance().logInfo("sourceDirectory = ${it}")
            if (filePath.startsWith(it)) {
                println("The file is outside the source directory.")
                // 从文件路径推断包名
                packageName = getPackageNameFromFilePath(filePath, it)
            }
        }

        return """
            package $packageName

            import androidx.lifecycle.LiveData
            import androidx.lifecycle.MutableLiveData
            import androidx.lifecycle.ViewModel

            class ${nameWithoutSuffix}ViewModel : ViewModel() {

                private val _text = MutableLiveData<String>().apply {
                    value = "This is notifications Fragment"
                }
                val text: LiveData<String> = _text
            }
        """.trimIndent()

    }

    private fun createKotlinFile(
        psiDirectory: PsiDirectory,
        fileName: String,
        fileContent: String
    ): PsiFile? {
        // 使用 PsiFileFactory 创建 Kotlin 文件
        val psiFile = PsiFileFactory.getInstance(psiDirectory.project)
            .createFileFromText(fileName, FileTypes.PLAIN_TEXT, fileContent)

        // 将文件保存到目录
        val createdFile = psiDirectory.add(psiFile)
        return createdFile as? PsiFile
    }
}