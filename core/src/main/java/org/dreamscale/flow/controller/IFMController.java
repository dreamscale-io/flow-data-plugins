package org.dreamscale.flow.controller;

import com.dreamscale.gridtime.api.event.EventType;
import com.dreamscale.gridtime.api.event.NewSnippetEventDto;
import com.dreamscale.gridtime.client.FlowClient;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.dreamscale.exception.ForbiddenException;
import org.dreamscale.feign.DefaultFeignConfig;
import org.dreamscale.flow.Logger;
import org.dreamscale.flow.activity.ActivityHandler;
import org.dreamscale.flow.activity.FlowPublisher;
import org.dreamscale.flow.activity.MessageQueue;
import org.dreamscale.jackson.ObjectMapperBuilder;
import org.dreamscale.logging.RequestResponseLoggerFactory;
import org.dreamscale.time.LocalDateTimeService;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class IFMController {

//    private static final String API_URL = "http://localhost:8080";
    private static final String API_URL = "https://torchie.dreamscale.io";

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
        if (isActive()) {
            try {
                flowClient.authPing();
            } catch (ForbiddenException ex) {
                flowClient = createFlowClient();
                flowPublisher.setFlowClient(flowClient);
            }
            try {
                flowClient.authPing();
            } catch (ForbiddenException ex) {
                throw new RuntimeException("Access denied, verify your API key is correct");
            }
            messageQueue.flush();
            flowPublisher.flush();
        }
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

        ApiSettings apiSettings = resolveApiSettings();

        return new DefaultFeignConfig()
                .jacksonFeignBuilder()
                .requestResponseLoggerFactory(new RequestResponseLoggerFactory())
                .requestInterceptor(new StaticAuthHeaderRequestInterceptor(apiSettings.getApiKey()))
                .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .target(FlowClient.class, apiSettings.getApiUrl());
    }

    private ApiSettings resolveApiSettings() {
        File apiSettingsFile = new File(getFlowDir(), "settings.json");
        if (apiSettingsFile.exists() == false) {
            throw new InvalidApiKeyException("Failed to resolve api settings from file=" + apiSettingsFile.getAbsolutePath());
        }

        try {
            String jsonStr = new String(Files.readAllBytes(apiSettingsFile.toPath()));

            ObjectMapper mapper = new ObjectMapperBuilder()
                    .jsr310TimeModule()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();

            return mapper.readValue(jsonStr, ApiSettings.class);
        } catch (IOException ex) {
            throw new InvalidApiKeyException("Failed to read api settings=" + apiSettingsFile.getAbsolutePath(), ex);
        }

    }

    public static String decrypt(String encryptedAPIKey) {
       //todo
        return "";
    }

    public void addSnippet(String source, String snippet) {
        NewSnippetEventDto snippetEvent = NewSnippetEventDto.builder()
                .source(source)
                .snippet(snippet)
                .position(LocalDateTime.now())
                .build();
        flowClient.publishSnippet(snippetEvent);
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

    private static class ApiSettings {
        private String apiKey;
        private String apiUrl;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
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
