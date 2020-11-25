package org.sonarlint.intellij.its;

import com.intellij.remoterobot.RemoteRobot;
import java.time.Duration;
import okhttp3.OkHttpClient;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.intellij.its.pages.WelcomeFrame;
import org.sonarlint.intellij.its.utils.StepsLogger;

import static com.intellij.remoterobot.fixtures.dataExtractor.TextDataPredicatesKt.startsWith;

public class SayHelloJavaTest {
    @BeforeClass
    public static void initLogging() {
        StepsLogger.init();
    }

    @Test
    void checkSayHello() throws InterruptedException {
        final RemoteRobot remoteRobot = new RemoteRobot("http://localhost:8082",
          new OkHttpClient.Builder()
            .readTimeout(Duration.ofMinutes(10))
            .connectTimeout(Duration.ofMinutes(10))
            .build());
        final WelcomeFrame welcomeFrame = remoteRobot.find(WelcomeFrame.class);
        assert (welcomeFrame.hasText(startsWith("Version 20")));
    }
}
