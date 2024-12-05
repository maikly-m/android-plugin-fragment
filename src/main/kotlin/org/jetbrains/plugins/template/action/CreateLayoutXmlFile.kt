package org.jetbrains.plugins.template.action

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.template.CustomFileLogger
import org.jetbrains.plugins.template.Utils.getModuleNameFromFile
import java.io.File
import java.nio.file.Paths

class CreateLayoutXmlFile {
    fun createFile(project: Project, folder: VirtualFile, name: String) {
        // 获取文件目录
        val psiDirectory = PsiManager.getInstance(project).findDirectory(folder) ?: return
        // 需要在layout文件夹下面处理
        // 使用 map 来处理每个字符 输出格式 abc_abc_gh
        val layoutNameSuffix = name.mapIndexed { _, it ->
            if (it.isUpperCase()) "_${it.lowercase()}" else it.toString()
        }.joinToString("")  // 将字符列表重新组合成字符串

        val layoutName = "fragment$layoutNameSuffix"

        // 获取项目的根路径
        val projectBasePath = project.basePath ?: return
        projectBasePath.split(File.separatorChar).let {
            it[it.lastIndex].run {
                val removeSuffix = projectBasePath.removeSuffix(this)
                val moduleName = getModuleNameFromFile(project, folder)

                val resourcesPath = Paths.get(removeSuffix, moduleName, "src", "main", "res")
                val layoutPath = resourcesPath.resolve("layout")
                CustomFileLogger.getInstance().logInfo("resourcesPath: ${resourcesPath}")

                // 获取 resources 目录和 layout 子目录
                val resourcesDir = LocalFileSystem.getInstance().findFileByPath(resourcesPath.toString())
                val layoutDir = resourcesDir?.findChild("layout")
                CustomFileLogger.getInstance().logInfo("layoutDir: ${layoutDir?.path}")
                // 如果 layout 文件夹不存在，创建它
                if (layoutDir == null) {
                    resourcesDir?.createChildDirectory(this, "layout")?.run {
                        createXmlFile(project, this, layoutName)
                    }
                } else {
                    createXmlFile(project, layoutDir, layoutName)
                }
            }
        }
    }
    private fun createXmlFile(project: Project, layoutDir: VirtualFile, layoutName: String) {
        // 获取文件目录
        val psiDirectory = PsiManager.getInstance(project).findDirectory(layoutDir) ?: return
        // 创建 XML 文件内容
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <androidx.constraintlayout.widget.ConstraintLayout 
                xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:app="http://schemas.android.com/apk/res-auto"
                xmlns:tools="http://schemas.android.com/tools"
                android:layout_width="match_parent"
                android:layout_height="match_parent" >

                <TextView
                    android:id="@+id/text_notifications"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:layout_marginTop="8dp"
                    android:layout_marginEnd="8dp"
                    android:textAlignment="center"
                    android:textSize="20sp"
                    app:layout_constraintBottom_toBottomOf="parent"
                    app:layout_constraintEnd_toEndOf="parent"
                    app:layout_constraintStart_toStartOf="parent"
                    app:layout_constraintTop_toTopOf="parent" />
            </androidx.constraintlayout.widget.ConstraintLayout >
        """.trimIndent()

        // 创建新 xml 文件
        WriteCommandAction.runWriteCommandAction(project) {
            // 创建新文件
            val newFile =
                writeFile(psiDirectory, "${layoutName}.xml", xmlContent)
            newFile?.let {
                // 刷新文件，以便在 IDE 中看到新文件
                FileDocumentManager.getInstance().saveAllDocuments()
            }
        }
        CustomFileLogger.getInstance().logInfo("Created XML file, done")
    }

    private fun writeFile(
        psiDirectory: PsiDirectory,
        fileName: String,
        fileContent: String
    ): PsiFile? {
        val psiFile = PsiFileFactory.getInstance(psiDirectory.project)
            .createFileFromText(fileName, FileTypes.PLAIN_TEXT, fileContent)
        CustomFileLogger.getInstance().logInfo("writeFile XML file, fileName:$fileName")
        // 将文件保存到目录
        val createdFile = psiDirectory.add(psiFile)
        return createdFile as? PsiFile
    }
}