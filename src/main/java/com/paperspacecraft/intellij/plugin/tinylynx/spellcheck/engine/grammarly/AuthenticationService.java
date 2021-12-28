package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.paperspacecraft.intellij.plugin.tinylynx.settings.SettingsService;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model.RedirectLocation;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
final class AuthenticationService {
    private static final Logger LOG = Logger.getInstance(AuthenticationService.class);

    private final Supplier<String> authSupplier = Suppliers.memoizeWithExpiration(this::retrieveAuthString, 60, TimeUnit.MINUTES);

    private final Project project;

    private static final String AUTH_SHORT_STRING_FORMAT = "gnar_containerId=%s;redirect_location=%s;";
    private static final String AUTH_STRING_FORMAT = AUTH_SHORT_STRING_FORMAT + "grauth=%s;csrf-token=%s;";
    private static final String AUTH_REQUEST_URL_FORMAT = "https://auth.grammarly.com/v3/user/oranonymous?app=firefoxExt&containerId=%s";

    private static final int STATUS_CODE_OK = 200;

    public AuthenticationService(Project project) {
        this.project = project;
    }

    public String getAuthString() {
        return authSupplier.get();
    }

    public Map<String, String> getRequestHeaders() {
        return ImmutableMap.<String, String>builder()
                .put("Accept-Encoding", "gzip, deflate, br")
                .put("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                .put("Cache-Control", "no-cache")
                .put("Origin", getSettings().getGrammarlyClientOrigin())
                .put("Pragma", "no-cache")
                .put("User-Agent", getSettings().getGrammarlyUserAgent())
                .build();
    }

    private SettingsService getSettings() {
        return SettingsService.getInstance(project);
    }

    @SneakyThrows(URISyntaxException.class)
    private String retrieveAuthString() {

        String containerId = RandomStringUtils.randomAlphanumeric(15);
        String redirectLocation = Base64.getEncoder().encodeToString(new Gson().toJson(RedirectLocation.INSTANCE).getBytes());
        String csrfToken = StringUtils.EMPTY;
        String grauth = StringUtils.EMPTY;

        String authRequestUrl = String.format(AUTH_REQUEST_URL_FORMAT, containerId);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(new URI(authRequestUrl));
        getRequestHeaders().forEach(requestBuilder::header);
        HttpRequest request = requestBuilder
                .header("X-Container-Id", containerId)
                .header("X-Client-Version", getSettings().getGrammarlyClientVersion())
                .header("X-Client-Type", getSettings().getGrammarlyClientType())
                .header("Cookie", String.format(AUTH_SHORT_STRING_FORMAT, containerId, redirectLocation) + getSettings().getGrammarlyCookie())
                .GET()
                .build();

        try {
            HttpResponse<?> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != STATUS_CODE_OK) {
                throw new IOException("Invalid response status: " + response.statusCode());
            }
            Map<String, String> cookies = extractCookies(response);
            csrfToken = cookies.getOrDefault("csrf-token", csrfToken);
            grauth = cookies.getOrDefault("grauth", csrfToken);
            if (StringUtils.isEmpty(csrfToken) || StringUtils.isEmpty(grauth)) {
                throw new IOException("Authentication cookie has not been received");
            }
        } catch (IOException e) {
            LOG.error(String.format("Could not complete request to %s", authRequestUrl), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return String.format(
                AUTH_STRING_FORMAT,
                containerId,
                redirectLocation,
                grauth,
                csrfToken);
    }

    private static Map<String, String> extractCookies(HttpResponse<?> response) {
        List<String> cookies = response.headers().allValues("set-cookie");
        if (cookies == null || cookies.isEmpty()) {
            return Collections.emptyMap();
        }
        return cookies
                .stream()
                .map(str -> StringUtils.substringBefore(str, ";"))
                .filter(str -> StringUtils.contains(str, "="))
                .collect(Collectors.toMap(str -> StringUtils.substringBefore(str, "="), str -> StringUtils.substringAfter(str, "=")));
    }
}
