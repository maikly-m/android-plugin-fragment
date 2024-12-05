package org.jetbrains.plugins.template.action
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.template.CustomFileLogger
import org.jetbrains.plugins.template.Utils
import java.io.File

class MyAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        // 在点击菜单项时弹出消息框
        Messages.showMessageDialog(
            e.project,
            "Tips",
            "My Log",
            Messages.getInformationIcon()
        )
    }
}