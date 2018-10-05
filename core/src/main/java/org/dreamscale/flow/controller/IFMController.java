package org.dreamscale.flow.controller;

import com.dreamscale.htmflow.api.event.EventType;
import com.dreamscale.htmflow.api.event.NewSnippetEvent;
import com.dreamscale.htmflow.client.FlowClient;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.dreamscale.feign.DefaultFeignConfig;
import org.dreamscale.flow.Logger;
import org.dreamscale.flow.activity.ActivityHandler;
import org.dreamscale.flow.activity.FlowPublisher;
import org.dreamscale.flow.activity.MessageQueue;
import org.dreamscale.time.LocalDateTimeService;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class IFMController {

    private static final String API_URL = "http://localhost:8080";
//    private static final String API_URL = "http://htmflow.dreamscale.io";

    private AtomicBoolean active = new AtomicBoolean(false);
    private ActivityHandler activityHandler;
    private MessageQueue messageQueue;
    private FlowPublisher flowPublisher;
    private FlowClient flowClient;
    private PushModificationActivityTimer pushModificationActivityTimer;

    public IFMController(Logger logger) {
        File ideaFlowDir = createFlowPluginDir();
        LocalDateTimeService timeService = new LocalDateTimeService();
        flowPublisher = new FlowPublisher(ideaFlowDir, logger, timeService);
        messageQueue = new MessageQueue(flowPublisher, timeService);
        activityHandler = new ActivityHandler(this, messageQueue, timeService);
        pushModificationActivityTimer = new PushModificationActivityTimer(activityHandler, 30);
    }

    private File getFlowDir() {
        return new File(System.getProperty("user.home"), ".flow");
    }

    private File createFlowPluginDir() {
        File flowPluginDir = new File(getFlowDir(), "com.jetbrains.intellij");
        flowPluginDir.mkdirs();
        return flowPluginDir;
    }

    public ActivityHandler getActivityHandler() {
        return activityHandler;
    }

    public void flushBatch() {
        messageQueue.flush();
        flowPublisher.flush();
    }

    public boolean isActive() {
        return active.get();
    }

    public boolean isInactive() {
        return active.get() == false;
    }

    public void start() {
        if (active.get() == false) {
            flowClient = createFlowClient();
            pushModificationActivityTimer.start();
            flowPublisher.start(flowClient);
            active.set(true);
        }
    }

    public void shutdown() {
        if (active.compareAndSet(true, false)) {
            pushModificationActivityTimer.cancel();
            messageQueue.pushEvent(EventType.DEACTIVATE, "IDE Shutdown");
        }
    }

    private FlowClient createFlowClient() {
        // TODO: make these configurable
        int connectTimeoutMillis = 5000;
        int readTimeoutMillis = 30000;
        String apiKey = resolveApiKey();
        return new DefaultFeignConfig()
                .jacksonFeignBuilder()
                .requestInterceptor(new StaticAuthHeaderRequestInterceptor(apiKey))
                .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .target(FlowClient.class, API_URL);
    }

    private String resolveApiKey() {
        File apiKeyFile = new File(getFlowDir(), "api.key");
        if (apiKeyFile.exists() == false) {
            throw new InvalidApiKeyException("Failed to resolve api key from file=" + apiKeyFile.getAbsolutePath());
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(apiKeyFile.toPath());
        } catch (Exception ex) {
            throw new InvalidApiKeyException("Failed to read api key from file=" + apiKeyFile.getAbsolutePath(), ex);
        }
        return lines.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElseThrow(() -> new InvalidApiKeyException("Failed to read api key from file=" + apiKeyFile.getAbsolutePath()));
    }

    public void addSnippet(String source, String snippet) {
        NewSnippetEvent snippetEvent = NewSnippetEvent.builder()
                .eventType(EventType.SNIPPET)
                .source(source)
                .snippet(snippet)
                .position(LocalDateTime.now())
                .build();
        flowClient.addSnippet(snippetEvent);
    }

    private static final class InvalidApiKeyException extends RuntimeException {
        InvalidApiKeyException(String message) {
            super(message);
        }

        InvalidApiKeyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class PushModificationActivityTimer {

        private Timer timer;
        private ActivityHandler activityHandler;
        private long intervalInSeconds;

        public PushModificationActivityTimer(ActivityHandler activityHandler, int intervalInSeconds) {
            this.activityHandler = activityHandler;
            this.intervalInSeconds = intervalInSeconds;
        }

        public void start() {
            if (timer != null) {
                timer.cancel();
            }

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    activityHandler.pushModificationActivity(intervalInSeconds);
                }
            };

            long intervalInMillis = intervalInSeconds * 1000;
            timer = new Timer();
            timer.scheduleAtFixedRate(timerTask, intervalInMillis, intervalInMillis);
        }

        public void cancel() {
            timer.cancel();
            timer = null;
        }

    }

    private static class StaticAuthHeaderRequestInterceptor implements RequestInterceptor {

        private String apiKey;

        public StaticAuthHeaderRequestInterceptor(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public void apply(RequestTemplate template) {
            template.header("X-API-KEY", apiKey);
        }

    }
}
