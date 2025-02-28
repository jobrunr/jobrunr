package org.jobrunr.tests.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.assertj.core.api.Assertions;
import org.jobrunr.tests.server.AbstractSimpleBackgroundJobServer;
import org.jobrunr.tests.server.SimpleBackgroundJobServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class DashboardActionsE2ETest extends AbstractPlaywrightE2ETest {

    private static AbstractSimpleBackgroundJobServer server;

    @BeforeAll
    static void beforeAll() {
        server = new SimpleBackgroundJobServer()
                .withGsonMapper()
                .withPaused();
        server.start();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void setUpNavigateToDashboard() {
        page.navigate("http://localhost:8000/dashboard/jobs");
    }

    @Test
    void canOpenTheJobsDashboardPage() {
        assertThat(jobTabButton()).containsText("33");
        assertThat(title("Enqueued jobs")).isVisible();

        Assertions.assertThat(jobTableRows().count()).isEqualTo(20);
        assertThat(jobTableRows().first()).containsText("an enqueued job");

        assertThat(jobsTablePagination()).containsText("1–20 of 33");
        assertThat(jobsTablePaginationPrevButton()).isDisabled();
        assertThat(jobsTablePaginationPrevButton()).hasAccessibleDescription("Go to previous page");
        assertThat(jobsTablePaginationNextButton()).isEnabled();
        assertThat(jobsTablePaginationNextButton()).hasAccessibleDescription("Go to next page");
    }

    @Test
    void canNavigateToScheduledJobs() {
        assertThat(scheduledMenuBtn()).containsText("1");
        scheduledMenuBtn().click();
        assertThat(title("Scheduled jobs")).isVisible();

        Assertions.assertThat(jobTableRows().count()).isEqualTo(1);
        assertThat(jobTableRows().first()).containsText("the job");

        assertThat(jobsTablePagination()).containsText("1–1 of 1");
        assertThat(jobsTablePaginationPrevButton()).isDisabled();
        assertThat(jobsTablePaginationNextButton()).isDisabled();
    }

    @Test
    void canNavigateToEnqueuedJobs() {
        assertThat(enqueuedMenuBtn()).containsText("33");
        enqueuedMenuBtn().click();
        assertThat(title("Enqueued jobs")).isVisible();

        Assertions.assertThat(jobTableRows().count()).isEqualTo(20);
        assertThat(jobTableRows().first()).containsText("an enqueued job");
    }

    public static void blockToDebugOnDashboard()  {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Thread.currentThread().interrupt()));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }



}
