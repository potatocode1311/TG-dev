package com.trivia_game.automation;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import java.net.URL;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import com.trivia_game.utilities.ApkPathUtil;

public class BaseTest {

    protected AndroidDriver driver;

    @BeforeTest
    public void setup() {
        try {
            UiAutomator2Options options = new UiAutomator2Options()
                    .setDeviceName("Pixel_5")
                    .setApp(ApkPathUtil.getApkPath())
                    .setAutomationName("UiAutomator2");

            driver = new AndroidDriver(
                    new URL("http://127.0.0.1:4723"),
                    options
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterTest
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
