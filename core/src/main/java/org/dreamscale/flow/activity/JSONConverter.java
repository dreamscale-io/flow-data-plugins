package org.dreamscale.flow.activity;

import com.dreamscale.gridtime.api.flow.activity.NewEditorActivityDto;
import com.dreamscale.gridtime.api.flow.activity.NewExecutionActivityDto;
import com.dreamscale.gridtime.api.flow.activity.NewExternalActivityDto;
import com.dreamscale.gridtime.api.flow.activity.NewModificationActivityDto;
import com.dreamscale.gridtime.api.flow.batch.NewFlowBatchEventDto;
import com.dreamscale.gridtime.api.flow.event.NewSnippetEventDto;
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
        idToClassMap.put("EditorActivity", NewEditorActivityDto.class);
        idToClassMap.put("ExecutionActivity", NewExecutionActivityDto.class);
        idToClassMap.put("ExternalActivity", NewExternalActivityDto.class);
        idToClassMap.put("ModificationActivity", NewModificationActivityDto.class);
        idToClassMap.put("Event", NewFlowBatchEventDto.class);
        idToClassMap.put("SnippetEvent", NewSnippetEventDto.class);
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
