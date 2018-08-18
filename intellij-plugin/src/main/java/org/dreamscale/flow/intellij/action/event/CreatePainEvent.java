package org.dreamscale.flow.intellij.action.event;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.dreamscale.flow.controller.IFMController;
import org.dreamscale.flow.intellij.IdeaFlowApplicationComponent;
import org.dreamscale.flow.intellij.action.ActionSupport;
import org.dreamscale.flow.state.TaskState;

import javax.swing.Icon;

public class CreatePainEvent extends AnAction {

	private Icon PAIN_ICON;
	private Icon PAIN_ICON_DOT1;
	private Icon PAIN_ICON_DOT2;
	private Icon PAIN_ICON_DOT3;


	public CreatePainEvent() {
		PAIN_ICON = IdeaFlowApplicationComponent.getIcon("pain.png");
		PAIN_ICON_DOT1 = IdeaFlowApplicationComponent.getIcon("pain_1dot.png");
		PAIN_ICON_DOT2 = IdeaFlowApplicationComponent.getIcon("pain_2dot.png");
		PAIN_ICON_DOT3 = IdeaFlowApplicationComponent.getIcon("pain_3dot.png");
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		IFMController controller = ActionSupport.getIFMController(e);
		if (controller != null) {
			Project project = e.getProject();
			Editor editor = e.getData(CommonDataKeys.EDITOR);
			VirtualFile file = e.getData(LangDataKeys.VIRTUAL_FILE);

			String painMessage = promptForInput(controller);
			if (painMessage != null) {
				String snippet = ActionSupport.getSelectedText(editor);
				if (snippet != null) {
					String source = ActionSupport.getActiveFilePath(project, file);
					controller.createPainSnippet(painMessage, source, snippet);
				} else {
					controller.createPain(painMessage);
				}

				ActionSupport.getTaskManager().updateTask(controller.getActiveTask());
			}
		}
	}

	@Override
	public void update(AnActionEvent e) {
		super.update(e);
		ActionSupport.disableWhenNotRecording(e);

		IFMController controller = IdeaFlowApplicationComponent.getIFMController();
		if (controller != null && controller.getActiveTask() != null) {
			TaskState activeTask = controller.getActiveTask();
			updateIcon(e.getPresentation(), activeTask.getUnresolvedPainCount());
		}
	}

	protected String promptForInput(IFMController controller) {
		String questionToAsk = determineQuestionToAsk(controller);

		return IdeaFlowApplicationComponent.promptForInput("WTF?!", questionToAsk);
	}

	private String determineQuestionToAsk(IFMController controller) {
		String questionToAsk;
		int painSize = 0;

		if (controller != null && controller.getActiveTask() != null) {
			TaskState activeTask = controller.getActiveTask();
			painSize = activeTask.getUnresolvedPainCount();
		}

		questionToAsk = "What are you confused about? (question)";

		return questionToAsk;
	}

	private void updateIcon(Presentation presentation, int unresolvedPainCount) {
		if (unresolvedPainCount < 1) {
			presentation.setIcon(PAIN_ICON);
		} else if (unresolvedPainCount == 1) {
			presentation.setIcon(PAIN_ICON_DOT1);
		} else if (unresolvedPainCount == 2) {
			presentation.setIcon(PAIN_ICON_DOT2);
		} else if (unresolvedPainCount > 2) {
			presentation.setIcon(PAIN_ICON_DOT3);
		}
	}

}
