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

class CreateFragmentFile {
    fun createFragmentFile(project: Project, folder: VirtualFile, name: String) {
        // 获取文件目录
        val psiDirectory = PsiManager.getInstance(project).findDirectory(folder) ?: return

        // 创建新 Kotlin 文件
        WriteCommandAction.runWriteCommandAction(project) {
            // 自动生成 Fragment 类代码
            val fragmentCode = generateFragmentCode(project, psiDirectory, folder, name)
            // 创建新文件
            val newFile = createKotlinFile(psiDirectory, "${name}Fragment.kt", fragmentCode)
            newFile?.let {
                // 刷新文件，以便在 IDE 中看到新文件
                FileDocumentManager.getInstance().saveAllDocuments()
            }
        }

    }
    private fun generateFragmentCode(
        project: Project,
        psiDirectory: PsiDirectory,
        folder: VirtualFile,
        fileName: String
    ): String {
        // 自动生成一个简单的 Fragment 代码模板
        val nameWithoutSuffix = fileName.removeSuffix(".kt")
        val fragmentName = nameWithoutSuffix + "Fragment"
        val binding = "Fragment"+nameWithoutSuffix+"Binding"
        val viewmodel = nameWithoutSuffix+"ViewModel"
        var packageName = "com.example"
        var applicationId = "com.example"

        // 获取当前文件夹的路径
        val filePath = folder.path
        val sourceDirectory = getSourceDirectory(project, filePath, folder)

        if (filePath.startsWith(sourceDirectory)) {
            CustomFileLogger.getInstance().logInfo("The file is outside the source directory.")
            // 从文件路径推断包名
            packageName = getPackageNameFromFilePath(filePath, sourceDirectory)
        }

        // 假设你正在获取应用模块的 build.gradle 文件
        val buildGradleFile = findBuildGradleFile(project, folder)

        if (buildGradleFile != null) {
            applicationId = getApplicationIdFromBuildGradle(buildGradleFile).toString()
            CustomFileLogger.getInstance().logInfo("The applicationId (package name) is: $applicationId")
        } else {
            CustomFileLogger.getInstance().logInfo("Couldn't find build.gradle file.")
        }

        return """
            package $packageName

            import android.os.Bundle
            import android.view.LayoutInflater
            import android.view.View
            import android.view.ViewGroup
            import androidx.fragment.app.Fragment
            import androidx.lifecycle.ViewModelProvider
            import $applicationId.databinding.$binding

            class $fragmentName : Fragment() {

                private var _binding: $binding? = null

                // This property is only valid between onCreateView and
                // onDestroyView.
                private val binding get() = _binding!!

                override fun onCreateView(
                    inflater: LayoutInflater,
                    container: ViewGroup?,
                    savedInstanceState: Bundle?
                ): View {
                    val viewModel =
                        ViewModelProvider(this).get($viewmodel::class.java)

                    _binding = $binding.inflate(inflater, container, false)
                    val root: View = binding.root
                    return root
                }

                override fun onDestroyView() {
                    super.onDestroyView()
                    _binding = null
                }
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