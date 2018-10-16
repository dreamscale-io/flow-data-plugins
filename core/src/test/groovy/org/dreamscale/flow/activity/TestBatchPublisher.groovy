package org.dreamscale.flow.activity

import com.dreamscale.htmflow.api.activity.NewEditorActivity
import com.dreamscale.htmflow.api.batch.NewBatchEvent
import com.dreamscale.htmflow.api.batch.NewFlowBatch
import com.dreamscale.htmflow.api.event.EventType
import com.dreamscale.htmflow.client.FlowClient
import org.dreamscale.exception.NotFoundException
import org.dreamscale.flow.Logger
import org.dreamscale.time.MockTimeService
import spock.lang.Specification

import java.time.LocalDateTime

class TestBatchPublisher extends Specification {

    FlowPublisher flowPublisher
    File tempDir
    JSONConverter jsonConverter = new JSONConverter()
    MockTimeService timeService = new MockTimeService()

    FlowClient mockFlowClient

    void setup() {
        tempDir = new File(File.createTempFile("temp", ".txt").parentFile, "queue-dir")
        tempDir.deleteDir()
        tempDir.mkdirs()

        Logger logger = Mock(Logger)
        flowPublisher = new FlowPublisher(tempDir, logger, timeService)

        mockFlowClient = Mock(FlowClient)
        flowPublisher.flowClientReference.set(mockFlowClient)
    }

    def cleanup() {
        tempDir.delete()

        List<File> batchFiles = flowPublisher.getBatchesToPublish()
        batchFiles.each { File file ->
            file.delete()
        }
    }

    def "commitBatch SHOULD create stuff to publish"() {
        given:
        File tmpFile = flowPublisher.getActiveFile()
        tmpFile << "some stuff"

        when:
        flowPublisher.commitActiveFile()

        then:
        assert flowPublisher.hasSomethingToPublish()
    }

    private NewEditorActivity createEditorActivity() {
        NewEditorActivity.builder()
                .endTime(LocalDateTime.now())
                .durationInSeconds(5)
                .filePath("hello.txt")
                .isModified(true)
                .build();
    }

    private File createBatchFile() {
        NewEditorActivity editorActivity = createEditorActivity()
        File tmpFile = flowPublisher.getActiveFile()
        tmpFile << jsonConverter.toJSON(editorActivity) + "\n"
        tmpFile
    }

    def "convertBatchFileToObject SHOULD create a Batch object that can be sent to the server"() {
        given:
        File tmpFile = createBatchFile()

        when:
        NewFlowBatch batch = flowPublisher.convertBatchFileToObject(tmpFile)

        then:
        assert batch != null
        assert batch.editorActivityList.size() == 1
    }

    def "convertBatchFileToObject SHOULD support events "() {
        given:
        NewBatchEvent batchEvent = NewBatchEvent.builder()
                .position(LocalDateTime.now())
                .type(EventType.NOTE)
                .comment("hello!")
                .build();


        File tmpFile = File.createTempFile("messages", ".flow")
        tmpFile << jsonConverter.toJSON(batchEvent) + "\n"

        when:
        NewFlowBatch batch = flowPublisher.convertBatchFileToObject(tmpFile)

        then:
        assert batch != null
        assert batch.eventList.size() == 1
    }

    def "publishBatches SHOULD send all batches to the server and delete files"() {
        given:
        createBatchFile()

        when:
        flowPublisher.commitActiveFile()
        flowPublisher.publishBatches()

        then:
        assert flowPublisher.hasSomethingToPublish() == false
        1 * mockFlowClient.addBatch(_)
    }

    def "publishBatches SHOULD mark file as failed if parsing fails"() {
        given:
        File file = flowPublisher.getActiveFile()
        file << "illegal json"

        when:
        flowPublisher.commitActiveFile()
        flowPublisher.publishBatches()

        then:
        File[] files = flowPublisher.failedDir.listFiles()
        assert files.length == 1
    }

    def "publishBatches should skip batch file which fails to publish"() {
        given:
        createBatchFile()
        mockFlowClient.addBatch(_) >> { throw new RuntimeException("Publication Failure") }

        when:
        flowPublisher.commitActiveFile()
        flowPublisher.publishBatches()

        then:
        assert flowPublisher.hasSomethingToPublish() == false
        assert flowPublisher.publishDir.listFiles().length == 1
    }

    def "publishBatches should set aside batches where the task cannot be found and resume on next session"() {
        given:
        createBatchFile()
        mockFlowClient.addBatch(_) >> { throw new NotFoundException("task not found") }

        when:
        flowPublisher.commitActiveFile()
        flowPublisher.publishBatches()

        then:
        assert flowPublisher.hasSomethingToPublish() == false

        and:
        assert flowPublisher.retryNextSessionDir.listFiles().length == 1

        when:
        flowPublisher.start(mockFlowClient)

        then:
        assert flowPublisher.hasSomethingToPublish() == true
    }

    def "publishBatches should delay retry of failed batch until tomorrow"() {
        given:
        int clientCallCount = 0
        createBatchFile()
        mockFlowClient.addBatch(_) >> {
            clientCallCount++
            throw new Exception("you lose!")
        }

        when:
        flowPublisher.commitActiveFile()
        flowPublisher.publishBatches()

        then:
        assert clientCallCount == 1
        assert flowPublisher.hasSomethingToPublish() == false
        assert flowPublisher.publishDir.listFiles().length == 1

        when:
        flowPublisher.publishBatches()

        then:
        assert clientCallCount == 1

        when:
        timeService.plusDays(2)
        flowPublisher.publishBatches()

        then:
        assert clientCallCount == 2
    }

}
