package com.github.zetten.test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.runner.Description;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.TWO_MINUTES;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

class Harness {
    private static final Logger LOG = LoggerFactory.getLogger(Harness.class);

    final DockerComposeContainer environment;

    private ChromeBrowserContainer chromeContainer;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.MILLISECONDS)
            .build();

    Harness() {
        this.environment = new DockerComposeContainer(Paths.get("src/test/resources/docker-compose.yml").toFile())
                .withExposedService("web_1", 80);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.environment.finished(null)));
    }

    void startEnvironment() {
        this.environment.starting(Description.EMPTY);

        LOG.info("*** Services exposed on: {} ***", getBaseUrl());

        // Some services can take time to start up, so just wait until we get a good web response
        with().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .and().atMost(TWO_MINUTES)
                .await("Test environment started")
                .until(() -> {
                    try {
                        Request webRoot = new Request.Builder().url(getBaseUrl()).build();
                        try (Response response = httpClient.newCall(webRoot).execute()) {
                            assertThat(response.code(), is(200));
                        }
                    } catch (Exception e) {
                        fail();
                    }
                });

        startBrowser();
    }

    void stopEnvironment() {
        this.stopBrowser();
        this.environment.finished(Description.EMPTY);
    }

    Response getResponse(String path) {
        try {
            Request request = new Request.Builder().url(getUrl(path)).build();
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String getUrl(String path) {
        return getBaseUrl() + path;
    }

    private String getBaseUrl() {
        return "http://" +
                environment.getServiceHost("web_1", 80) + ":" +
                environment.getServicePort("web_1", 80);
    }

    private void startBrowser() {
        this.chromeContainer = new ChromeBrowserContainer(environment);
        this.chromeContainer.preScenario();
    }

    private void stopBrowser() {
        this.chromeContainer.postScenario();
    }

    WebDriver getChromeWebDriver() {
        return this.chromeContainer.getWebDriver();
    }

    private static final class ChromeBrowserContainer extends BrowserWebDriverContainer {
        ChromeBrowserContainer(DockerComposeContainer env) {
            super();

            try {
                withDesiredCapabilities(DesiredCapabilities.chrome());

                Path recordingPath = Paths.get("build/recording").toAbsolutePath();
                Files.createDirectories(recordingPath);

                withRecordingMode(VncRecordingMode.RECORD_ALL, recordingPath.toFile());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void preScenario() {
            super.starting(Description.EMPTY);
        }

        void postScenario() {
            super.succeeded(Description.EMPTY);
            super.finished(Description.EMPTY);
        }
    }

}
