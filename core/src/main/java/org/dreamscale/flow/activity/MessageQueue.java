package org.dreamscale.flow.activity;

import com.dreamscale.gridtime.api.flow.activity.NewEditorActivityDto;
import com.dreamscale.gridtime.api.flow.activity.NewExecutionActivityDto;
import com.dreamscale.gridtime.api.flow.activity.NewExternalActivityDto;
import com.dreamscale.gridtime.api.flow.activity.NewModificationActivityDto;
import com.dreamscale.gridtime.api.flow.batch.NewFlowBatchEventDto;
import com.dreamscale.gridtime.api.flow.event.EventType;
import org.dreamscale.time.TimeService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MessageQueue {

    private MessageLogger messageLogger;
    private TimeService timeService;

    public MessageQueue(FlowPublisher batchPublisher, TimeService timeService) {
        this(new FileMessageLogger(batchPublisher, timeService), timeService);
    }

    public MessageQueue(MessageLogger messageLogger, TimeService timeService) {
        this.messageLogger = messageLogger;
        this.timeService = timeService;
    }

    public void flush() {
        messageLogger.flush();
    }

    public void pushEditorActivity(Long durationInSeconds, String filePath, boolean isModified) {
        pushEditorActivity(durationInSeconds, timeService.now(), filePath, isModified);
    }

    public void pushEditorActivity(Long durationInSeconds, LocalDateTime endTime, String filePath, boolean isModified) {
        NewEditorActivityDto activity = NewEditorActivityDto.builder()
                .endTime(endTime)
                .durationInSeconds(durationInSeconds)
                .filePath(filePath)
                .isModified(isModified)
                .build();

        messageLogger.writeMessage(activity);
    }

    public void pushModificationActivity(Long durationInSeconds, int modificationCount) {
        NewModificationActivityDto activity = NewModificationActivityDto.builder()
                .endTime(timeService.now())
                .durationInSeconds(durationInSeconds)
                .modificationCount(modificationCount)
                .build();

        messageLogger.writeMessage(activity);
    }

    public void pushExecutionActivity(Long durationInSeconds, String processName,
                                      int exitCode,
                                      String executionTaskType,
                                      boolean isDebug) {
        NewExecutionActivityDto activity = NewExecutionActivityDto.builder()
                .durationInSeconds(durationInSeconds)
                .endTime(timeService.now())
                .processName(processName)
                .exitCode(exitCode)
                .executionTaskType(executionTaskType)
                .isDebug(isDebug)
                .build();

        messageLogger.writeMessage(activity);
    }

    public void pushExternalActivity(Long durationInSeconds, String comment) {
        NewExternalActivityDto activity = NewExternalActivityDto.builder()
                .endTime(timeService.now())
                .durationInSeconds(durationInSeconds)
                .comment(comment)
                .build();

        messageLogger.writeMessage(activity);
    }

    public void pushEvent(EventType eventType, String message) {
        NewFlowBatchEventDto batchEvent = NewFlowBatchEventDto.builder()
                .position(timeService.now())
                .type(eventType)
                .comment(message)
                .build();

        messageLogger.writeMessage(batchEvent);
    }


    static class FileMessageLogger implements MessageLogger {
        private TimeService timeService;
        private FlowPublisher batchPublisher;
        private Map<Long, File> activeMessageFiles = new HashMap<>();

        private final Object lock = new Object();
        private JSONConverter jsonConverter = new JSONConverter();

        private LocalDateTime lastBatchTime;
        private int messageCount;

        private final int BATCH_TIME_LIMIT_IN_SECONDS = 30 * 60;
        private final int BATCH_MESSAGE_LIMIT = 500;

        FileMessageLogger(FlowPublisher batchPublisher, TimeService timeService) {
            this.batchPublisher = batchPublisher;
            this.timeService = timeService;

            lastBatchTime = timeService.now();
        }

        public void flush() {
            startNewBatch();
        }

        public void writeMessage(Object message) {
            try {
                String messageAsJson = jsonConverter.toJSON(message);

                synchronized (lock) {
                    if (isBatchThresholdReached()) {
                        startNewBatch();
                    }
                    File file = batchPublisher.getActiveFile();
                    appendLineToFile(file, messageAsJson);
                    messageCount++;
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private void appendLineToFile(File file, String text) throws IOException {
            try (PrintWriter printWriter = new PrintWriter(new FileWriter(file, true))) {
                printWriter.println(text);
            }
        }

        private boolean isBatchThresholdReached() {
            Duration duration = Duration.between(lastBatchTime, timeService.now());
            return messageCount > 0 && ((duration.getSeconds() > BATCH_TIME_LIMIT_IN_SECONDS) ||
                    messageCount > BATCH_MESSAGE_LIMIT);
        }

        private void startNewBatch() {
            synchronized (lock) {
                batchPublisher.commitActiveFile();
                activeMessageFiles.clear();

                lastBatchTime = timeService.now();
                messageCount = 0;
            }
        }

    }

}
