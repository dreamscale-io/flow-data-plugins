package org.dreamscale.flow.intellij.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.dreamscale.flow.controller.IFMController;
import org.dreamscale.flow.intellij.IdeaFlowApplicationComponent;
import org.dreamscale.flow.intellij.handler.VirtualFileActivityHandler;

public class ActionSupport {

    public static IFMController getIFMController(AnActionEvent e) {
        IFMController controller = null;
        if (e != null && e.getProject() != null) {
            controller = IdeaFlowApplicationComponent.getIFMController();
        }
        return controller;
    }

    public static String getSelectedText(Editor editor) {
        if (editor == null) {
            return null;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        return selectionModel.getSelectedText();
    }

    public static String getActiveFilePath(Project project, VirtualFile file) {
        String fileName = null;

        if (file != null) {
            fileName = VirtualFileActivityHandler.getFullFilePathOrDefault(file, project, file.getName());
        }
        return fileName;
    }

}
