package org.dreamscale.flow.intellij;

import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.UIBundle;
import com.intellij.util.messages.MessageBusConnection;
import org.dreamscale.flow.controller.IFMController;
import org.dreamscale.flow.intellij.handler.DeactivationHandler;
import org.dreamscale.flow.intellij.handler.VirtualFileActivityHandler;

import javax.swing.Icon;

public class IdeaFlowApplicationComponent extends ApplicationComponent.Adapter {

    public static final Logger log = new Logger();

    private static final String NAME = "IdeaFlow.Component";

    private IFMController controller;
    private MessageBusConnection appConnection;
    private VirtualFileActivityHandler virtualFileActivityHandler;

    public static IdeaFlowApplicationComponent getApplicationComponent() {
        return (IdeaFlowApplicationComponent) ApplicationManager.getApplication().getComponent(NAME);
    }

    public static IFMController getIFMController() {
        return getApplicationComponent().controller;
    }

    public static VirtualFileActivityHandler getFileActivityHandler() {
        return getApplicationComponent().virtualFileActivityHandler;
    }

    public static Icon getIcon(String path) {
        return IconLoader.getIcon("/icons/" + path, IdeaFlowApplicationComponent.class);
    }

    public static String promptForInput(String title, String message) {
        return Messages.showInputDialog(message, UIBundle.message(title), Messages.getQuestionIcon());
    }

    public static void showErrorMessage(String title, String message) {
        Messages.showErrorDialog(message, title);
    }


    @Override
    public String getComponentName() {
        return NAME;
    }

    @Override
    public void initComponent() {
        controller = new IFMController(log);
        virtualFileActivityHandler = new VirtualFileActivityHandler(controller.getActivityHandler());

        try {
            controller.start();
        } catch (Exception ex) {
            // TODO: this should be a message popup to the user
            log.error("Disabling @torchie Flow Metrics Plugin due to controller initialization failure: " + ex.getMessage(), ex.getCause());
        }
         
        ApplicationListener applicationListener = new ApplicationListener(controller);
        appConnection = ApplicationManager.getApplication().getMessageBus().connect();
        appConnection.subscribe(ApplicationActivationListener.TOPIC, applicationListener);
    }

    @Override
    public void disposeComponent() {
        if (controller != null) {
            controller.shutdown();
        }
        if (appConnection != null) {
            appConnection.disconnect();
        }
    }

    private static class ApplicationListener extends ApplicationActivationListener.Adapter {

        private DeactivationHandler deactivationHandler;

        ApplicationListener(IFMController controller) {
            deactivationHandler = new DeactivationHandler(controller);
        }

        @Override
        public void applicationActivated(IdeFrame ideFrame) {
            if (ideFrame.getProject() != null) {
                if (deactivationHandler.isPromptingForIdleTime() == false) {
                    deactivationHandler.markActiveFileEventAsIdleIfDeactivationThresholdExceeded(ideFrame.getProject());
                }
            }
        }

        @Override
        public void applicationDeactivated(IdeFrame ideFrame) {
            if (ideFrame.getProject() != null) {
                deactivationHandler.deactivated();
            }
        }

    }

}
