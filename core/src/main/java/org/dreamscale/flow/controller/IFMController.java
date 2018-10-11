package org.dreamscale.flow.controller;

import com.dreamscale.htmflow.api.event.EventType;
import com.dreamscale.htmflow.client.BatchClient;
import feign.Request;
import org.dreamscale.feign.JacksonFeignBuilder;
import org.dreamscale.flow.Logger;
import org.dreamscale.flow.activity.ActivityHandler;
import org.dreamscale.flow.activity.BatchPublisher;
import org.dreamscale.flow.activity.MessageQueue;
import org.dreamscale.time.LocalDateTimeService;

import java.io.File;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

public class IFMController {

    private boolean paused = true;
    private ActivityHandler activityHandler;
    private MessageQueue messageQueue;
    private BatchPublisher batchPublisher;

    public IFMController(Logger logger) {
        File ideaFlowDir = createIdeaFlowDir();
        LocalDateTimeService timeService = new LocalDateTimeService();
        batchPublisher = new BatchPublisher(ideaFlowDir, logger, timeService);
        messageQueue = new MessageQueue(batchPublisher, timeService);
        activityHandler = new ActivityHandler(this, messageQueue, timeService);
        startPushModificationActivityTimer(30);
    }

    private File createIdeaFlowDir() {
        File ideaFlowDir = new File(System.getProperty("user.home") + File.separator + ".ideaflow");
        ideaFlowDir.mkdirs();
        return ideaFlowDir;
    }

    private void startPushModificationActivityTimer(final long intervalInSeconds) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                getActivityHandler().pushModificationActivity(intervalInSeconds);
            }
        };

        long intervalInMillis = intervalInSeconds * 1000;
        new Timer().scheduleAtFixedRate(timerTask, intervalInMillis, intervalInMillis);
    }

    public ActivityHandler getActivityHandler() {
        return activityHandler;
    }

    public void flushBatch() {
        messageQueue.flush();
        batchPublisher.flush();
    }

    public void initClients(String apiUrl, String apiKey) {
        // TODO: make these configurable
        int connectTimeoutMillis = 5000;
        int readTimeoutMillis = 300000;
        BatchClient batchClient = new JacksonFeignBuilder()
                .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .target(BatchClient.class, apiUrl);
        batchPublisher.setBatchClient(batchClient);
    }

    public Duration getRecentIdleDuration() {
        return getActivityHandler().getRecentIdleDuration();
    }

    public boolean isRecording() {
        return paused == false;
    }

    public void shutdown() {
        messageQueue.pushEvent(EventType.DEACTIVATE, "IDE Shutdown");
    }

}
