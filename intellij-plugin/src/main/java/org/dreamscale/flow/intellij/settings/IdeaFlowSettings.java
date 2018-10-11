package org.dreamscale.flow.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;

//@State(
//		name = "org.openmastery.ideaflow.settings",
//		storages = @Storage(id = "other", file = StoragePathMacros.APP_CONFIG + "/ideaflow.xml")
//)
public class IdeaFlowSettings implements PersistentStateComponent<IdeaFlowSettings> {

    public static IdeaFlowSettings getInstance() {
        return ServiceManager.getService(IdeaFlowSettings.class);
    }

    private String apiUrl = "http://ideaflowdx.openmastery.org";
    private String apiKey;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public IdeaFlowSettings getState() {
        return this;
    }

    @Override
    public void loadState(IdeaFlowSettings ideaFlowSettings) {
        XmlSerializerUtil.copyBean(ideaFlowSettings, this);
    }


}
