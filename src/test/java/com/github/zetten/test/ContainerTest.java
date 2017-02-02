package com.github.zetten.test;

import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.FIVE_SECONDS;
import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 */
public class ContainerTest {

    private Harness harness;

    @Before
    public void setUp() {
        harness = new Harness();
        harness.startEnvironment();
    }

    @After
    public void tearDown() {
        harness.stopEnvironment();
    }

    @Test
    public void containerTest() {
        try (ResponseBody responseBody = harness.getResponse("/index.html").body()) {
            assertThat(responseBody.string(), containsString("Selenium test page"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        WebDriver webDriver = harness.getChromeWebDriver();
        webDriver.get(harness.getUrl("/index.html"));
        waitForVisibleElement(webDriver, "test-header");
        assertThat(webDriver.getTitle(), is("Selenium test page"));
    }

    public void waitForVisibleElement(WebDriver browser, String elementId) {
        with().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .and().atMost(FIVE_SECONDS)
                .await("Page element visible: " + elementId)
                .until(() -> browser.findElements(By.id(elementId)).size() > 0);
    }


}
