package org.dreamscale.flow.activity

import com.dreamscale.htmflow.api.activity.NewEditorActivity
import com.dreamscale.htmflow.api.batch.NewBatchEvent
import com.dreamscale.htmflow.api.batch.NewIFMBatch
import com.dreamscale.htmflow.api.event.EventType
import com.dreamscale.htmflow.client.FlowClient
import org.dreamscale.exception.NotFoundException
import org.dreamscale.flow.Logger
import org.dreamscale.time.MockTimeService
import spock.lang.Specification

import java.time.LocalDateTime

class TestBatchPublisher extends Specification {

    BatchPublisher batchPublisher
    File tempDir
    JSONConverter jsonConverter = new JSONConverter()
    MockTimeService timeService = new MockTimeService()

    FlowClient mockFlowClient

    void setup() {
        tempDir = new File(File.createTempFile("temp", ".txt").parentFile, "queue-dir")
        tempDir.deleteDir()
        tempDir.mkdirs()

        Logger logger = Mock(Logger)
        batchPublisher = new BatchPublisher(tempDir, logger, timeService)

        mockFlowClient = Mock(FlowClient)
        batchPublisher.flowClientReference.set(mockFlowClient)
    }

    def cleanup() {
        tempDir.delete()

        List<File> batchFiles = batchPublisher.getBatchesToPublish()
        batchFiles.each { File file ->
            file.delete()
        }
    }

    def "commitBatch SHOULD create stuff to publish"() {
        given:
        File tmpFile = batchPublisher.getActiveFile()
        tmpFile << "some stuff"

        when:
        batchPublisher.commitActiveFile()

        then:
        assert batchPublisher.hasSomethingToPublish()
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
        File tmpFile = batchPublisher.getActiveFile()
        tmpFile << jsonConverter.toJSON(editorActivity) + "\n"
        tmpFile
    }

    def "convertBatchFileToObject SHOULD create a Batch object that can be sent to the server"() {
        given:
        File tmpFile = createBatchFile()

        when:
        NewIFMBatch batch = batchPublisher.convertBatchFileToObject(tmpFile)

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


        File tmpFile = File.createTempFile("messages", ".log")
        tmpFile << jsonConverter.toJSON(batchEvent) + "\n"

        when:
        NewIFMBatch batch = batchPublisher.convertBatchFileToObject(tmpFile)

        then:
        assert batch != null
        assert batch.eventList.size() == 1
    }

    def "publishBatches SHOULD send all batches to the server and delete files"() {
        given:
        createBatchFile()

        when:
        batchPublisher.commitActiveFile()
        batchPublisher.publishBatches()

        then:
        assert batchPublisher.hasSomethingToPublish() == false
        1 * mockFlowClient.addIFMBatch(_)
    }

    def "publishBatches SHOULD mark file as failed if parsing fails"() {
        given:
        File file = batchPublisher.getActiveFile()
        file << "illegal json"

        when:
        batchPublisher.commitActiveFile()
        batchPublisher.publishBatches()

        then:
        File[] files = batchPublisher.failedDir.listFiles()
        assert files.length == 1
    }

    def "publishBatches should skip batch file which fails to publish"() {
        given:
        createBatchFile()
        mockFlowClient.addIFMBatch(_) >> { throw new RuntimeException("Publication Failure") }

        when:
        batchPublisher.commitActiveFile()
        batchPublisher.publishBatches()

        then:
        assert batchPublisher.hasSomethingToPublish() == false
        assert batchPublisher.publishDir.listFiles().length == 1
    }

    def "publishBatches should set aside batches where the task cannot be found and resume on next session"() {
        given:
        createBatchFile()
        mockFlowClient.addIFMBatch(_) >> { throw new NotFoundException("task not found") }

        when:
        batchPublisher.commitActiveFile()
        batchPublisher.publishBatches()

        then:
        assert batchPublisher.hasSomethingToPublish() == false

        and:
        assert batchPublisher.retryNextSessionDir.listFiles().length == 1

        when:
        batchPublisher.start(mockFlowClient)

        then:
        assert batchPublisher.hasSomethingToPublish() == true
    }

    def "publishBatches should delay retry of failed batch until tomorrow"() {
        given:
        int clientCallCount = 0
        createBatchFile()
        mockFlowClient.addIFMBatch(_) >> {
            clientCallCount++
            throw new Exception("you lose!")
        }

        when:
        batchPublisher.commitActiveFile()
        batchPublisher.publishBatches()

        then:
        assert clientCallCount == 1
        assert batchPublisher.hasSomethingToPublish() == false
        assert batchPublisher.publishDir.listFiles().length == 1

        when:
        batchPublisher.publishBatches()

        then:
        assert clientCallCount == 1

        when:
        timeService.plusDays(2)
        batchPublisher.publishBatches()

        then:
        assert clientCallCount == 2
    }

}
