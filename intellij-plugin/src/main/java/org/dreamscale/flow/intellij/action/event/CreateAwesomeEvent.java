package org.dreamscale.flow.intellij.action.event;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.dreamscale.flow.intellij.action.ActionSupport;
import org.dreamscale.flow.controller.IFMController;
import org.dreamscale.flow.intellij.IdeaFlowApplicationComponent;

public class CreateAwesomeEvent extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		IFMController controller = ActionSupport.getIFMController(e);
		if (controller != null) {
			Project project = e.getProject();
			Editor editor = e.getData(CommonDataKeys.EDITOR);
			VirtualFile file = e.getData(LangDataKeys.VIRTUAL_FILE);

			String awesomeMessage = promptForInput(controller);

			if (awesomeMessage != null) {
				String snippet = ActionSupport.getSelectedText(editor);
				if (snippet == null) {
					controller.resolveWithYay(awesomeMessage);
				} else {
					String source = ActionSupport.getActiveFilePath(project, file);
					controller.resolveWithAwesomeSnippet(awesomeMessage, source, snippet);
				}

				ActionSupport.getTaskManager().updateTask(controller.getActiveTask());
			}
		}
	}

	private String promptForInput(IFMController controller) {
//		List<String> unresolvedPainList = controller.getActiveTask().getTroubleshootingEventList();
//
//		String wtfString = "";
//		for (int i = 0; i < unresolvedPainList.size(); i++) {
//			String wtfMessage = unresolvedPainList.get(i);
//			wtfString += "-- $i: " + wtfMessage + "\n";
//		}

		return IdeaFlowApplicationComponent.promptForInput("YAY!", "What did you figure out? (#done to resolve)");
	}

	@Override
	public void update(AnActionEvent e) {
		super.update(e);
		ActionSupport.disableWhenNotRecording(e);
	}

}