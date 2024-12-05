package org.jetbrains.plugins.template.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.template.Utils.isValidString

//import com.intellij.psi.PsiClass

class FragmentCreateFileAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFile: VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

        // 判断文件是否为文件夹
        if (virtualFile != null && virtualFile.isDirectory) {
            e.presentation.isEnabledAndVisible = true  // 启用并显示 Action
        } else {
            e.presentation.isEnabledAndVisible = false // 禁用 Action
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        // Use EDT (Event Dispatch Thread) for UI-related updates like enabling/disabling actions
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        // 获取当前项目和文件夹路径
        val project = e.project ?: return
        val folder = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        // 弹出输入框让用户输入名字
        val inputName = Messages.showInputDialog(
            project,
            "Enter the fragment name:",
            "New Fragment",
            Messages.getQuestionIcon()
        ) ?: return  // 如果用户取消，直接返回

        if (inputName.isEmpty()) {
            Messages.showErrorDialog("Input file name!", "Error")
            return
        }

        // 检测输入是否为纯英文字符
        if (isValidString(inputName)) {
        } else {
            Messages.showErrorDialog("Input file name!(English indeed)", "Error")
            return
        }

        // 将第一个字符变为大写
        val capitalized = inputName.replaceFirstChar {
            if (it.isLowerCase()) it.uppercase() else it.toString()
        }

        // 创建 Kotlin 文件
        CreateFragmentFile().createFragmentFile(project, folder, capitalized)
        CreateViewModelFile().createFile(project, folder, capitalized)
        CreateLayoutXmlFile().createFile(project, folder, capitalized)
    }

}
