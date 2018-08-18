package org.dreamscale.flow.intellij.action.event;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.dreamscale.flow.controller.IFMController;
import org.dreamscale.flow.intellij.action.ActionSupport;

public class FlushBatchEvent extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        IFMController controller = ActionSupport.getIFMController(anActionEvent);
        if (controller != null) {
            controller.flushBatch();
        }
    }

}
