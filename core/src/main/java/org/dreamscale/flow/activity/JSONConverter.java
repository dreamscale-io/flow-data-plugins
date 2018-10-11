package org.dreamscale.flow.activity;

import com.dreamscale.htmflow.api.activity.NewEditorActivity;
import com.dreamscale.htmflow.api.activity.NewExecutionActivity;
import com.dreamscale.htmflow.api.activity.NewExternalActivity;
import com.dreamscale.htmflow.api.activity.NewIdleActivity;
import com.dreamscale.htmflow.api.activity.NewModificationActivity;
import com.dreamscale.htmflow.api.batch.NewBatchEvent;
import com.dreamscale.htmflow.api.event.NewSnippetEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dreamscale.jackson.ObjectMapperBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JSONConverter {

    Map<String, Class> idToClassMap = createIdToClassMap();

    Map<Class, String> classToIdMap = createClassToIdMap();

    ObjectMapper mapper;

    JSONConverter() {
        mapper = new ObjectMapperBuilder()
                .jsr310TimeModule()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private Map<String, Class> createIdToClassMap() {
        Map<String, Class> idToClassMap = new HashMap<String, Class>();
        idToClassMap.put("EditorActivity", NewEditorActivity.class);
        idToClassMap.put("ExecutionActivity", NewExecutionActivity.class);
        idToClassMap.put("ExternalActivity", NewExternalActivity.class);
        idToClassMap.put("ModificationActivity", NewModificationActivity.class);
        idToClassMap.put("IdleActivity", NewIdleActivity.class);
        idToClassMap.put("Event", NewBatchEvent.class);
        idToClassMap.put("SnippetEvent", NewSnippetEvent.class);
        return idToClassMap;
    }

    private Map<Class, String> createClassToIdMap() {
        Map<Class, String> classToIdMap = new HashMap<Class, String>();
        for (Map.Entry<String, Class> entry : createIdToClassMap().entrySet()) {
            classToIdMap.put(entry.getValue(), entry.getKey());
        }
        return classToIdMap;
    }

    String toJSON(Object object) throws JsonProcessingException {
        String typeName = classToIdMap.get(object.getClass());
        if (typeName == null) {
            throw new UnsupportedObjectType("Unable to find typeName for " + object.getClass().getName());
        }
        return typeName + "=" + mapper.writeValueAsString(object);
    }

    Object fromJSON(String jsonInString) throws IOException {
        int index = jsonInString.indexOf("=");

        String typeName = jsonInString.substring(0, index);
        String jsonContent = jsonInString.substring(index + 1, jsonInString.length());

        Class clazz = idToClassMap.get(typeName);
        return mapper.readValue(jsonContent, clazz);
    }

    static class UnsupportedObjectType extends RuntimeException {

        UnsupportedObjectType(String message) {
            super(message);
        }
    }
}
