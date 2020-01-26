package org.dreamscale.flow.intellij.action.event;

//import com.intellij.debugger.actions.ViewAsGroup;
//import com.intellij.debugger.engine.JavaValue;
import com.intellij.execution.impl.ConsoleViewUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.dreamscale.flow.controller.IFMController;
import org.dreamscale.flow.intellij.Logger;
import org.dreamscale.flow.intellij.action.ActionSupport;

import java.util.List;

public class CreateSnippetEvent extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        IFMController controller = ActionSupport.getIFMController(e);
        if (controller != null) {
            Project project = e.getProject();
            Editor editor = e.getData(CommonDataKeys.EDITOR);

            if (editor != null) {
                VirtualFile file = e.getData(LangDataKeys.VIRTUAL_FILE);
                boolean isConsole = ConsoleViewUtil.isConsoleViewEditor(editor);

                String snippet = ActionSupport.getSelectedText(editor);
                String source = isConsole ? "Console" : ActionSupport.getActiveFilePath(project, file);
                controller.addSnippet(source, snippet);
            } else {
                String snippet = getDebuggerSnippet(e);
                if (snippet != null) {
                    controller.addSnippet("Debugger", snippet);
                }
            }
        }
    }

    private String getDebuggerSnippet(AnActionEvent e) {
        return "";
        //TODO figure out how to do this in a version compatible way, APIs changed...
//        StringBuilder snippetBuilder = new StringBuilder(1000);
//        List<JavaValue> values = ViewAsGroup.getSelectedValues(e);
//        for (JavaValue value : values) {
//            if (snippetBuilder.length() > 0) {
//                snippetBuilder.append("\n");
//            }
//            snippetBuilder.append(value.getDescriptor().toString());
//        }
//        return snippetBuilder.length() > 0 ? snippetBuilder.toString() : null;
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        ActionSupport.disableWhenNotRecording(e);
    }

}
