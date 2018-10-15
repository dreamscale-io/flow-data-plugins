package org.dreamscale.flow.intellij.action.event;

import com.intellij.execution.impl.ConsoleViewUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.dreamscale.flow.controller.IFMController;
import org.dreamscale.flow.intellij.action.ActionSupport;

public class CreateSnippetEvent extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        IFMController controller = ActionSupport.getIFMController(e);
        if (controller != null) {
            Project project = e.getProject();
            Editor editor = e.getData(CommonDataKeys.EDITOR);
            VirtualFile file = e.getData(LangDataKeys.VIRTUAL_FILE);
            boolean isConsole = ConsoleViewUtil.isConsoleViewEditor(editor);

            String snippet = ActionSupport.getSelectedText(editor);
            String source = null;

            if (isConsole) {
                source = "Console";
            } else {
                source = ActionSupport.getActiveFilePath(project, file);
            }
            controller.addSnippet(source, snippet);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        ActionSupport.disableWhenNotRecording(e);
    }

}
