package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.paperspacecraft.intellij.plugin.tinylynx.settings.SettingsService;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckResult;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckTask;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckWorker;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model.Alert;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model.InitialMessage;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model.ServiceResponse;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model.Submission;
import lombok.AccessLevel;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GrammarlyWorker extends SpellcheckWorker {
    private static final Logger LOG = Logger.getInstance("com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine");

    private static final URI GRAMMARLY_ENDPOINT = URI.create("wss://capi.grammarly.com/freews");

    private static final int STATUS_GOING_AWAY = 1001;

    private static final Gson GSON = new Gson();

    private static final String EXTRA_WHITESPACES_PATTERN = "\\s{2,}";
    private static final String JSON_NODE_PATTERN = "\"\\w+\":(?:\"[^\"]+\"|-?\\d+),";


    private SpellcheckTask currentTask;
    private String currentDebugToken;
    private List<SpellcheckAlert> currentAlerts;
    private StringBuilder currentResponse;

    public GrammarlyWorker(Project project, Supplier<SpellcheckTask> taskSupplier) {
        super(project, taskSupplier);
        currentTask = taskSupplier.get();
    }

    /* ----------------
       Main logic start
       ---------------- */

    public void run() {
        if (currentTask == null) {
            return;
        }

        SettingsService settings = SettingsService.getInstance(getProject());
        AuthenticationService authentication = getProject().getService(AuthenticationService.class);

        currentDebugToken = createDebugToken(currentTask);
        currentAlerts = new ArrayList<>();
        currentResponse = new StringBuilder();

        String authString = getProject().getService(AuthenticationService.class).getAuthString();
        LOG.debug(String.format("[%s] Starting task %s", currentDebugToken, currentTask.getText()));
        LOG.debug(String.format("[%s] Using auth string %s", currentDebugToken, authString));

        WebSocketClient webSocketClient = new WebSocketClient();
        webSocketClient.setOnSocketOpenAction(this::onSockedOpened);
        webSocketClient.setOnConnectionEstablishedAction(this::onConnectionEstablished);
        webSocketClient.setOnAlertAction(this::onAlertReceived);
        webSocketClient.setOnFinishedAction(this::onTaskFinished);
        webSocketClient.setOnErrorAction(this::onTaskError);
        webSocketClient.setDebugToken(currentDebugToken);
        webSocketClient.setResponseAccumulator(currentResponse);

        WebSocket.Builder builder = HttpClient.newHttpClient().newWebSocketBuilder();
        authentication.getRequestHeaders().forEach(builder::header);
        builder.header("Cookie", settings.getGrammarlyCookie() + authString);
        builder.buildAsync(GRAMMARLY_ENDPOINT, webSocketClient).join();

        waitForCompletion();
    }

    /* --------------------
        Websocket callbacks
        ------------------- */

    private void onSockedOpened(WebSocket webSocket) {
        LOG.debug(String.format("[%s] Sending initial message", currentDebugToken));
        webSocket.sendText(GSON.toJson(InitialMessage.INSTANCE), true);
    }

    private void onConnectionEstablished(WebSocket webSocket) {
        LOG.debug(String.format("[%s] Sending text for analysis", currentDebugToken));
        webSocket.sendText(GSON.toJson(new Submission(currentTask.getText())), true);
    }

    private void onAlertReceived(SpellcheckAlert alert) {
        currentAlerts.add(alert);
    }

    private void onTaskFinished(WebSocketClient client, WebSocket webSocket) {
        String logString = currentResponse.toString().trim();
        // Fix for log block not being properly wrapped in UI
        logString = logString.replaceAll(JSON_NODE_PATTERN, "$0 ");

        currentTask.complete(new SpellcheckResult(
                currentTask.getText(),
                currentAlerts,
                logString));

        currentTask = getTaskSupplier().get();

        if (currentTask != null) {
            currentDebugToken = createDebugToken(currentTask);
            currentAlerts = new ArrayList<>();
            currentResponse = new StringBuilder();

            client.setDebugToken(currentDebugToken);
            client.setResponseAccumulator(currentResponse);
            LOG.debug(String.format("[%s] Recharging current worker for '%s'", currentDebugToken, currentTask.getText()));

            webSocket.sendText(GSON.toJson(InitialMessage.INSTANCE), true);

        } else {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, StringUtils.EMPTY);
            reportCompletion();
        }
    }

    private void onTaskError(WebSocket webSocket) {
        webSocket.sendClose(STATUS_GOING_AWAY, StringUtils.EMPTY);
        reportCompletion();
    }

    /* ---------------
       Utility methods
       --------------- */

    private static String createDebugToken(SpellcheckTask task) {
        return StringUtils.abbreviate(
                task.getText().replaceAll(EXTRA_WHITESPACES_PATTERN, StringUtils.SPACE),
                50);
    }

    /* ----------------
       Websocket client
       ---------------- */

    @Setter
    private static class WebSocketClient implements WebSocket.Listener {

        private static final List<String> PASS_THROUGH_ACTIONS = Collections.singletonList("emotions");

        private Consumer<WebSocket> onSocketOpenAction;

        private Consumer<WebSocket> onConnectionEstablishedAction;

        private Consumer<SpellcheckAlert> onAlertAction;

        private BiConsumer<WebSocketClient, WebSocket> onFinishedAction;

        private Consumer<WebSocket> onErrorAction;

        private String debugToken;

        private StringBuilder responseAccumulator;

        @Setter(value = AccessLevel.PRIVATE)
        private State state;

        @Override
        public void onOpen(WebSocket webSocket) {
            LOG.debug(String.format("[%s] Socket opened", debugToken));
            state = State.SOCKED_OPENED;
            onSocketOpenAction.accept(webSocket);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            LOG.debug(String.format("[%s] Close command received. Status %s, reason '%s'", debugToken, statusCode, reason));
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (data != null) {
                processTextResponse(webSocket, data);
            } else {
                LOG.warn(String.format("[%s] Empty response", debugToken));
                state = State.ERROR;
                onErrorAction.accept(webSocket);
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        private void processTextResponse(WebSocket webSocket, CharSequence data) {
            ServiceResponse serviceResponse = deserialize(data, ServiceResponse.EMPTY, ServiceResponse.class);
            String action = serviceResponse.getAction();

            if (state == State.SOCKED_OPENED && "start".equals(action)) {
                LOG.debug(String.format("[%s] Initialization confirmed", debugToken));
                state = State.CONN_ESTABLISHED;
                onConnectionEstablishedAction.accept(webSocket);

            } else if (state == State.CONN_ESTABLISHED && "submit_ot".equals(action)) {
                LOG.debug(String.format("[%s] Submit action confirmed", debugToken));
                state = State.SUBMIT_CONFIRMED;

            } else if (state == State.SUBMIT_CONFIRMED && "alert".equals(action)) {
                LOG.debug(String.format("[%s] Alert received: %s", debugToken, data));

                SpellcheckAlert alert = deserialize(data, Alert.class);
                if (alert != null) {
                    responseAccumulator.append("\n").append(data);
                    onAlertAction.accept(alert);
                }

            } else if (state == State.SUBMIT_CONFIRMED && "finished".equals(action)) {
                LOG.debug(String.format("[%s] Checking finished: %s", debugToken, data));

                responseAccumulator.append("\n").append(data);
                onFinishedAction.accept(this, webSocket);

                state = State.SOCKED_OPENED;

            } else if ("error".equals(action)) {
                LOG.warn(String.format("[%s] Error: %s", debugToken, serviceResponse.getError()));
                state = State.ERROR;
                onErrorAction.accept(webSocket);

            } else if (!PASS_THROUGH_ACTIONS.contains(action)) {
                LOG.warn(String.format("[%s] Illegal data response type '%s' for the state '%s'", debugToken, action, state));
                state = State.ERROR;
                onErrorAction.accept(webSocket);
            }
        }

        @Override
        public void onError(WebSocket webSocket, Throwable e) {
            LOG.error("Error communicating via socket", e);
            state = State.ERROR;
            onErrorAction.accept(webSocket);
            WebSocket.Listener.super.onError(webSocket, e);
        }

        @SuppressWarnings("SameParameterValue")
        private static <T> T deserialize(CharSequence value, Class<T> type) {
            return deserialize(value, null, type);
        }

        private static <T> T deserialize(CharSequence value, T fallbackValue, Class<T> type) {
            try {
                return GSON.fromJson(value.toString(), type);
            } catch (JsonSyntaxException e) {
                LOG.warn(String.format("Could not deserialize value '%s'", value));
            }
            return fallbackValue;
        }
    }

    private enum State {
        IDLE, SOCKED_OPENED, CONN_ESTABLISHED, SUBMIT_CONFIRMED, RESPONSE_RECEIVED, ERROR
    }

}
