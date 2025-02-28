package org.jobrunr.tests.e2e;

import com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions;
import org.jobrunr.tests.server.AbstractSimpleBackgroundJobServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jobrunr.tests.e2e.PlaywrightAssertions.assertThat;

public abstract class AbstractDashboardActionsE2ETest extends AbstractPlaywrightE2ETest {

    protected static AbstractSimpleBackgroundJobServer server;

    @AfterAll
    static void afterAll() {
        server.stop();
    }


    @BeforeEach
    void setUpNavigateToDashboard() {
        page.navigate("http://localhost:8000/dashboard/jobs");
        page.waitForLoadState();
        assertThat(jobTabButton()).containsText("33", new ContainsTextOptions().setTimeout(20_000));
    }

    @Test
    void canOpenTheJobsDashboardPage() {
        assertThat(jobTabButton()).containsText("33");
        assertThat(title("Enqueued jobs")).isVisible();

        assertThat(jobTableRows().count()).isEqualTo(20);
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
        page.waitForLoadState();

        assertThat(title("Scheduled jobs")).isVisible();

        assertThat(jobTableRows().first()).containsText("the job");
        assertThat(jobTableRows().count()).isEqualTo(1);

        assertThat(jobsTablePagination()).containsText("1–1 of 1");
        assertThat(jobsTablePaginationPrevButton()).isDisabled();
        assertThat(jobsTablePaginationNextButton()).isDisabled();
    }

    @Test
    void canNavigateToEnqueuedJobs() {
        assertThat(enqueuedMenuBtn()).containsText("33");
        enqueuedMenuBtn().click();
        assertThat(title("Enqueued jobs")).isVisible();

        assertThat(jobTableRows().first()).containsText("an enqueued job");
        assertThat(jobTableRows().count()).isEqualTo(20);
    }

    @Test
    void canNavigateToProcessingJobs() {
        assertThat(processingMenuBtn()).containsText("0");
        processingMenuBtn().click();
        page.waitForLoadState();

        assertThat(title("Jobs being processed")).isVisible();

        assertThat(noJobsFoundMessage()).isVisible();
        assertThat(jobTable()).not().isVisible();
    }

    @Test
    void canNavigateToSucceededJobs() {
        assertThat(succeededMenuBtn()).containsText("2");
        succeededMenuBtn().click();
        page.waitForLoadState();

        assertThat(title("Succeeded jobs")).isVisible();

        assertThat(jobTableRows().first()).containsText("a succeeded job");
        assertThat(jobTableRows().count()).isEqualTo(2);

        assertThat(jobsTablePagination()).containsText("1–2 of 2");
        assertThat(jobsTablePaginationPrevButton()).isDisabled();
        assertThat(jobsTablePaginationNextButton()).isDisabled();
    }

    @Test
    void canNavigateToFailedJobs() {
        assertThat(failedMenuBtn()).containsText("1");
        failedMenuBtn().click();
        page.waitForLoadState();

        assertThat(title("Failed jobs")).isVisible();

        assertThat(jobTableRows().first()).containsText("failed job");
        assertThat(jobTableRows().count()).isEqualTo(1);

        assertThat(jobsTablePagination()).containsText("1–1 of 1");
        assertThat(jobsTablePaginationPrevButton()).isDisabled();
        assertThat(jobsTablePaginationNextButton()).isDisabled();
    }

    @Test
    void canNavigateToTheDetailsOfAJob() {
        failedMenuBtn().click();
        page.waitForLoadState();

        assertThat(title("Failed jobs")).isVisible();

        jobTableRows().first().locator("td a").first().click();
        assertThat(jobIdTitle()).isVisible();

        assertThat(breadcrumb()).containsText("Failed jobs");
        assertThat(jobNameTitle()).containsText("failed job");
        assertThat(jobHistoryPanel()).isVisible();

        assertThat(jobHistoryPanelItems().count()).isEqualTo(44);
        assertThat(jobHistoryPanelItems().first()).containsText("Job scheduled");
        jobHistorySortDescBtn().click();
        assertThat(jobHistoryPanelItems().first()).containsText("Job processing failed");
    }

    @Test
    void canNavigateToRecurringJobsPage() {
        recurringJobsTabBtn().click();
        page.waitForLoadState();

        assertThat(recurringJobsTabBtn().locator("span.MuiChip-label")).containsText("2");
    }

    @Test
    void canNavigateToServersPage() {
        serversTabBtn().click();
        page.waitForLoadState();

        assertThat(serversTabBtn().locator("span.MuiChip-label")).containsText("1");
    }

    public static void blockToDebugOnDashboard() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Thread.currentThread().interrupt()));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
