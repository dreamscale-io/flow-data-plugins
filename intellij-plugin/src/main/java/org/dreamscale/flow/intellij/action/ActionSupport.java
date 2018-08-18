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
import org.dreamscale.flow.intellij.settings.IdeaFlowSettings;
import org.dreamscale.flow.intellij.settings.IdeaFlowSettingsTaskManager;
import org.dreamscale.flow.state.TaskState;

public class ActionSupport {

	public static IFMController getIFMController(AnActionEvent e) {
		IFMController controller = null;
		if (e != null && e.getProject() != null) {
			controller = IdeaFlowApplicationComponent.getIFMController();
		}
		return controller;
	}

	public static String getActiveIdeaFlowName(AnActionEvent e) {
		IFMController controller = getIFMController(e);
		return controller == null ? null : controller.getActiveTaskName();
	}

	public static TaskState getActiveTask(AnActionEvent e) {
		IFMController controller = getIFMController(e);
		return controller == null ? null : controller.getActiveTask();
	}

	public static boolean isTaskActive(AnActionEvent e) {
		IFMController controller = getIFMController(e);
		return controller != null && controller.isTaskActive();
	}

	public static boolean isRecording(AnActionEvent e) {
		IFMController controller = getIFMController(e);
		return controller != null && controller.isRecording();
	}

	public static boolean isTaskActiveAndRecording(AnActionEvent e) {
		return isRecording(e) && isTaskActive(e);
	}

	public static boolean isPaused(AnActionEvent e) {
		IFMController controller = getIFMController(e);
		return controller != null && controller.isPaused();
	}

	public static String getSelectedText(Editor editor) {
		if (editor == null) {
			return null;
		}

		SelectionModel selectionModel = editor.getSelectionModel();
		return selectionModel.getSelectedText();
	}

	public static void disableWhenNotRecording(AnActionEvent e) {
		Presentation presentation = e.getPresentation();
		presentation.setEnabled(isTaskActiveAndRecording(e));
	}

	public static String getActiveFilePath(Project project, VirtualFile file) {
		String fileName = null;

		if (file != null) {
			fileName = VirtualFileActivityHandler.getFullFilePathOrDefault(file, project, file.getName());
		}
		return fileName;
	}

	public static IdeaFlowSettingsTaskManager getTaskManager() {
		return IdeaFlowSettings.getInstance().getTaskManager();
	}

}
