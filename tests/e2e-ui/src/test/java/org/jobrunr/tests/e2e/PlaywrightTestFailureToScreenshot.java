package org.jobrunr.tests.e2e;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Path;

public class PlaywrightTestFailureToScreenshot implements AfterTestExecutionCallback {


    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        if (!context.getTestInstance().isPresent() || !(context.getTestInstance().get() instanceof AbstractPlaywrightE2ETest instance)) {
            throw new RuntimeException("You're doing it wrong: this JUnit Extension can only work together with a playwright test");
        }

        if (context.getExecutionException().isPresent()) {
            var fileName = "/tmp/reports/playwright-exception-log-" + context.getDisplayName().replace(" ", "").replace("(", "").replace(")", "") + ".png";

            System.out.println("Saving Playwright state screenshot to " + fileName);
            try {
                instance.page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(fileName)));
            } catch (Exception e) {
                // WHY: This can happen on the CI server, for instance permission problems
                System.out.println("Exception while saving screenshot to " + fileName);
                e.printStackTrace();
            }
            System.out.println("Saved Playwright state screenshot to " + fileName);
        }
    }
}