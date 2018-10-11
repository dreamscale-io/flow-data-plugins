package org.dreamscale.flow.activity;

import org.dreamscale.flow.controller.IFMController;
import org.dreamscale.time.TimeService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ActivityHandler {

    private static final int SHORTEST_ACTIVITY = 3;

    private IFMController controller;
    private MessageQueue messageQueue;
    private TimeService timeService;
    private FileActivity activeFileActivity;
    private AtomicInteger modificationCount = new AtomicInteger(0);

    private Duration recentIdleDuration = null;

    private Map<Long, ProcessActivity> activeProcessMap = new HashMap<>();

    public ActivityHandler(IFMController controller, MessageQueue messageQueue, TimeService timeService) {
        this.controller = controller;
        this.messageQueue = messageQueue;
        this.timeService = timeService;
    }

    public Duration getRecentIdleDuration() {
        return recentIdleDuration;
    }

    private boolean isSame(String newFilePath) {
        return isDifferent(newFilePath) == false;
    }

    private boolean isDifferent(String newFilePath) {
        if (activeFileActivity == null) {
            return newFilePath != null;
        } else {
            return activeFileActivity.filePath.equals(newFilePath) == false;
        }
    }

    private boolean isOverActivityThreshold() {
        return activeFileActivity != null && activeFileActivity.getDurationInSeconds() >= SHORTEST_ACTIVITY;
    }

    public void markIdleTime(final Duration idleDuration) {
        markIdleOrExternal(idleDuration, () -> messageQueue.pushIdleActivity(idleDuration.getSeconds()));
    }

    public void markExternalActivity(final Duration idleDuration, final String comment) {
        recentIdleDuration = idleDuration;
        markIdleOrExternal(idleDuration, () -> messageQueue.pushExternalActivity(idleDuration.getSeconds(), comment));
    }

    private void markIdleOrExternal(Duration idleDuration, Runnable block) {
        if (idleDuration.getSeconds() >= SHORTEST_ACTIVITY) {
            if (activeFileActivity != null) {
                long duration = activeFileActivity.getDurationInSeconds() - idleDuration.getSeconds();
                if (duration > 0) {
                    LocalDateTime endTime = timeService.now().minusSeconds((int) idleDuration.getSeconds());
                    messageQueue.pushEditorActivity(duration, endTime, activeFileActivity.filePath, activeFileActivity.modified);
                }
            }
            block.run();
            if (activeFileActivity != null) {
                activeFileActivity = createFileActivity(activeFileActivity.filePath);
            }
        }
    }

    public void markProcessStarting(Long processId, String processName, String executionTaskType, boolean isDebug) {
        ProcessActivity processActivity = new ProcessActivity(processName, executionTaskType, timeService, isDebug);
        activeProcessMap.put(processId, processActivity);
        //TODO this will leak memory if the processes started are never closed
    }

    public void markProcessEnding(Long processId, int exitCode) {
        ProcessActivity processActivity = activeProcessMap.remove(processId);
        if (processActivity != null) {
            messageQueue.pushExecutionActivity(processActivity.getDurationInSeconds(),
                                               processActivity.processName, exitCode, processActivity.executionTaskType, processActivity.isDebug);
        }
    }

    public void startFileEvent(String filePath) {
        if (isDifferent(filePath)) {
            if (isOverActivityThreshold()) {
                messageQueue.pushEditorActivity(activeFileActivity.getDurationInSeconds(),
                                                activeFileActivity.filePath, activeFileActivity.modified);
            }

            activeFileActivity = createFileActivity(filePath);
        }
    }

    public void endFileEvent(String filePath) {
        if ((filePath == null) || isSame(filePath)) {
            startFileEvent(null);
        }
    }

    public void fileModified(String filePath) {
        if (activeFileActivity != null && activeFileActivity.filePath.equals(filePath)) {
            activeFileActivity.modified = true;
        }
        modificationCount.incrementAndGet();
    }

    public void pushModificationActivity(Long intervalInSeconds) {
        int modCount = modificationCount.getAndSet(0);
        if (modCount > 0) {
            messageQueue.pushModificationActivity(intervalInSeconds, modCount);
        }
    }

    private FileActivity createFileActivity(String filePath) {
        return filePath == null ? null : new FileActivity(filePath, timeService, false);
    }


    private static class ProcessActivity {

        private TimeService timeService;
        private LocalDateTime timeStarted;
        private String processName;
        private String executionTaskType;
        private boolean isDebug;

        public ProcessActivity(String processName, String executionTaskType, TimeService timeService, boolean isDebug) {
            this.processName = processName;
            this.executionTaskType = executionTaskType;
            this.timeService = timeService;
            this.timeStarted = timeService.now();
            this.isDebug = isDebug;
        }

        public long getDurationInSeconds() {
            return Duration.between(timeStarted, timeService.now()).toMillis() / 1000;
        }

        public String toString() {
            return "ProcessActivity [processName=" + processName + ", executionTaskType=" +
                    executionTaskType + ", " + "duration=" + getDurationInSeconds() + ", isDebug=" + isDebug + "]";
        }
    }

    private static class FileActivity {

        private LocalDateTime time;
        private TimeService timeService;
        private String filePath;
        private boolean modified;

        public FileActivity(String filePath, TimeService timeService, boolean modified) {
            this.filePath = filePath;
            this.timeService = timeService;
            this.time = timeService.now();
            this.modified = modified;
        }

        public long getDurationInSeconds() {
            return Duration.between(time, timeService.now()).toMillis() / 1000;
        }

        public String toString() {
            return "FileActivity [path=" + filePath + ", modified=" + modified + ", duration=" + getDurationInSeconds() + "]";
        }
    }

}
