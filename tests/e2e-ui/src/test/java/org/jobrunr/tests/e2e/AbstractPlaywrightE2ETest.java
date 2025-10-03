package org.jobrunr.tests.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;

@ExtendWith(PlaywrightTestFailureToScreenshot.class)
public class AbstractPlaywrightE2ETest {

    static Playwright playwright;
    static Browser browser;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void beforeAll() {
        playwright = Playwright.create(new Playwright.CreateOptions());
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void afterAll() {
        playwright.close();
    }

    @BeforeEach
    final void setUpPlaywrightContextAndPage() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080));
        page = context.newPage();
    }

    @AfterEach
    final void tearDownPlaywrightContext() {
        context.close();
    }

    protected void takeScreenshot(String fileName) {
        page.screenshot(new Page.ScreenshotOptions().setPath(Path.of(fileName)));
    }

    protected Locator byRoleWithText(AriaRole role, String text) {
        return page.locator("[role=" + role.name().toLowerCase() + "]", new Page.LocatorOptions().setHas(page.getByText(text)));
    }

    protected Locator jobMenuOption(String title) {
        return page.getByTitle(title, new Page.GetByTitleOptions().setExact(true));
    }

    protected Locator dashboardTabBtn() {
        return page.locator("#dashboard-btn");
    }

    protected Locator jobTabButton() {
        return page.locator("#jobs-btn");
    }

    protected Locator title(String name) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(name));
    }

    protected Locator awaitingMenuBtn() {
        return page.locator("#awaiting-menu-btn");
    }

    protected Locator scheduledMenuBtn() {
        return page.locator("#scheduled-menu-btn");
    }

    protected Locator carbonIntensityChart() {
        return page.locator(".carbon-intensity-chart");
    }

    protected Locator enqueuedMenuBtn() {
        return page.locator("#enqueued-menu-btn");
    }

    protected Locator processingMenuBtn() {
        return page.locator("#processing-menu-btn");
    }

    protected Locator succeededMenuBtn() {
        return page.locator("#succeeded-menu-btn");
    }

    protected Locator failedMenuBtn() {
        return page.locator("#failed-menu-btn");
    }

    protected Locator jobsTabBtn() {
        return page.locator("#jobs-btn");
    }

    protected Locator recurringJobsTabBtn() {
        return page.locator("#recurring-jobs-btn");
    }

    protected Locator serversTabBtn() {
        return page.locator("#servers-btn");
    }

    protected Locator breadcrumb() {
        return page.locator("#breadcrumb");
    }

    protected Locator jobTable() {
        return page.locator("#jobs-table");
    }

    protected Locator jobsTablePagination() {
        return page.locator("#jobs-table-pagination");
    }

    protected Locator jobsTablePaginationPrevButton() {
        return jobsTablePagination().locator("button:first-of-type");
    }

    protected Locator jobsTablePaginationNextButton() {
        return jobsTablePagination().locator("button:last-of-type");
    }

    protected Locator jobTableRows() {
        return jobTable().locator("tbody>tr");
    }

    protected void jobTableRowsClickOnFirstJob() {
        jobTableRows().first().locator("td a").first().click();
    }

    protected Locator noJobsFoundMessage() {
        return page.locator("#no-jobs-found-message");
    }

    protected Locator jobIdTitle() {
        return page.locator("#job-id-title");
    }

    protected Locator jobNameTitle() {
        return page.locator("#job-name-title");
    }

    protected Locator jobHistoryPanel() {
        return page.locator("#job-history-panel");
    }

    protected Locator jobHistoryPanelItems() {
        return jobHistoryPanel().locator("div.MuiAccordion-root");
    }

    protected Locator jobHistorySortAscBtn() {
        return page.locator("#jobhistory-sort-asc-btn");
    }

    protected Locator jobHistorySortDescBtn() {
        return page.locator("#jobhistory-sort-desc-btn");
    }
}
