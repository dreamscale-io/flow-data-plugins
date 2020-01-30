package org.dreamscale.flow.activity

import com.dreamscale.gridtime.api.flow.activity.NewEditorActivityDto
import com.dreamscale.gridtime.api.flow.batch.NewFlowBatchEventDto
import spock.lang.Specification

import java.time.LocalDateTime

class TestJSONConverter extends Specification {

    JSONConverter converter = new JSONConverter()

    def "toJSON/fromJSON SHOULD serialize/deserialize API types"() {
        given:
        NewEditorActivityDto editorActivity = NewEditorActivityDto.builder()
                .endTime(LocalDateTime.now())
                .durationInSeconds(5)
                .filePath("hello.txt")
                .isModified(true)
                .build()

        when:
        String json = converter.toJSON(editorActivity)
        NewEditorActivityDto deserializedActivity = (NewEditorActivityDto) converter.fromJSON(json)

        then:
        assert deserializedActivity != null
    }

    def "fromJSON SHOULD not explode if = sign in comments"() {
        given:
        NewFlowBatchEventDto event = NewFlowBatchEventDto.builder()
                .comment("This is a comment about an == sign that I screwed up")
                .build()

        when:
        String json = converter.toJSON(event)
        NewFlowBatchEventDto deserializedEvent = (NewFlowBatchEventDto) converter.fromJSON(json)

        then:
        assert deserializedEvent != null
    }

    def "toJSON SHOULD not explode if serialization type is not in the type map"() {
        given:
        Object o = new Object()

        when:
        converter.toJSON(o)

        then:
        thrown(JSONConverter.UnsupportedObjectType.class)
    }


}
