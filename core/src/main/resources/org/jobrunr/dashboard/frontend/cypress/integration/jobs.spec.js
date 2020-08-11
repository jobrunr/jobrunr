context('Actions', () => {
    beforeEach(() => {
        cy.visit('http://localhost:8000/dashboard/jobs');
        cy.viewport(1280, 800);
        waitForSSE();
    });


    const scheduledMenuBtn = () => cy.get('#scheduled-menu-btn');
    const enqueuedMenuBtn = () => cy.get('#enqueued-menu-btn');
    const processingMenuBtn = () => cy.get('#processing-menu-btn');
    const succeededMenuBtn = () => cy.get('#succeeded-menu-btn');
    const failedMenuBtn = () => cy.get('#failed-menu-btn');

    const jobsTabBtn = () => cy.get('#jobs-btn');
    const recurringJobsTabBtn = () => cy.get('#recurring-jobs-btn');
    const serversTabBtn = () => cy.get('#servers-btn');
    const breadcrumb = () => cy.get('#breadcrumb');
    const title = () => cy.get('#title');

    const jobTable = () => cy.get('#jobs-table');
    const jobTableRows = () => jobTable().get('tbody>tr');
    const noJobsFoundMessage = () => cy.get('#no-jobs-found-message');

    const jobIdTitle = () => cy.get('#job-id-title');
    const jobNameTitle = () => cy.get('#job-name-title');

    const jobHistoryPanel = () => cy.get('#job-history-panel');
    const jobHistoryPanelItems = () => jobHistoryPanel().find('div.MuiExpansionPanel-root');
    const jobHistorySortAscBtn = () => cy.get('#jobhistory-sort-asc-btn');
    const jobHistorySortDescBtn = () => cy.get('#jobhistory-sort-desc-btn');

    it('It opens the jobs dashboard page', () => {
        jobsTabBtn().get('span.MuiChip-label').should('contain', '33');
        title().should('contain', 'Enqueued jobs');

        jobTableRows().should('have.length', 20);
        jobTableRows().eq(0).should('contain', 'an enqueued job');
        jobTablePagination().should('contain', '1-20 of 33');
        jobTablePagination().previousButton().should('have.attr', 'title', 'Previous page').and('be.disabled');
        jobTablePagination().nextButton().should('have.attr', 'title', 'Next page').and('be.enabled');
    });


    it('It can navigate to the scheduled jobs', () => {
        scheduledMenuBtn().should('contain', '1');
        scheduledMenuBtn().click();
        title().should('contain', 'Scheduled jobs');

        jobTableRows().should('have.length', 1);
        jobTableRows().eq(0).should('contain', 'the job');
        jobTablePagination().should('contain', '1-1 of 1');
        jobTablePagination().previousButton().should('have.attr', 'title', 'Previous page').and('be.disabled');
        jobTablePagination().nextButton().should('have.attr', 'title', 'Next page').and('be.disabled');
    });

    it('It can navigate to the enqueued jobs', () => {
        enqueuedMenuBtn().should('contain', '33');
        enqueuedMenuBtn().click();
        title().should('contain', 'Enqueued jobs');

        jobTableRows().should('have.length', 20);
        jobTableRows().eq(0).should('contain', 'an enqueued job');
        jobTablePagination().should('contain', '1-20 of 33');
        jobTablePagination().previousButton().should('have.attr', 'title', 'Previous page').and('be.disabled');
        jobTablePagination().nextButton().should('have.attr', 'title', 'Next page').and('be.enabled');
    });

    it('It can navigate to the processing jobs', () => {
        processingMenuBtn().should('contain', '0');
        processingMenuBtn().click();
        title().should('contain', 'Jobs being processed');

        noJobsFoundMessage().should('be.visible')
        jobTable().should('not.exist');
    });

    it('It can navigate to the succeeded jobs', () => {
        succeededMenuBtn().should('contain', '2');
        succeededMenuBtn().click();
        title().should('contain', 'Succeeded jobs');

        jobTableRows().should('have.length', 2);
        jobTableRows().eq(0).should('contain', 'a succeeded job');
        jobTablePagination().should('contain', '1-2 of 2');
        jobTablePagination().previousButton().should('have.attr', 'title', 'Previous page').and('be.disabled');
        jobTablePagination().nextButton().should('have.attr', 'title', 'Next page').and('be.disabled');
    });

    it('It can navigate to the failed jobs', () => {
        failedMenuBtn().should('contain', '1');
        failedMenuBtn().click();
        title().should('contain', 'Failed jobs');

        jobTableRows().should('have.length', 1);
        jobTableRows().eq(0).should('contain', 'failed job');
        jobTablePagination().should('contain', '1-1 of 1');
        jobTablePagination().previousButton().should('have.attr', 'title', 'Previous page').and('be.disabled');
        jobTablePagination().nextButton().should('have.attr', 'title', 'Next page').and('be.disabled');
    });

    it('It can navigate to the details of a job', () => {
        failedMenuBtn().click();
        title().should('contain', 'Failed jobs');
        jobTableRows().should('have.length', 1);

        jobTableRows().eq(0).should('contain', 'failed job');
        jobTableRows().eq(0).find('td a').eq(0).click();
        jobIdTitle().should('be.visible');

        breadcrumb().should('contain', 'Failed jobs');
        jobNameTitle().should('contain', 'failed job');
        jobHistoryPanel().should('be.visible');

        jobHistoryPanelItems().should('have.length', 44);
        jobHistoryPanelItems().eq(0).should('contain', 'Job scheduled');
        jobHistorySortDescBtn().click();
        jobHistoryPanelItems().eq(0).should('contain', 'Job processing failed');
    });

    it('It can navigate to the recurring jobs page', () => {
        recurringJobsTabBtn().click();
        recurringJobsTabBtn().get('span.MuiChip-label').should('contain', '2');
    });

    it('It can navigate to the servers page', () => {
        serversTabBtn().click();
        serversTabBtn().get('span.MuiChip-label').should('contain', '1');
    });


    const jobTablePagination = function () {
        let paginationSelector = cy.get('#jobs-table-pagination');
        Object.assign(Object.getPrototypeOf(paginationSelector), {
            previousButton() {
                return paginationSelector.find('button').eq(0);
            },
            nextButton() {
                return paginationSelector.find('button').eq(1);
            }
        });
        return paginationSelector;
    }

    const waitForSSE = function () {
        jobsTabBtn().get('span.MuiChip-label', {timeout: 20000}).should(($chip) => {
            expect($chip.text()).contains('33');
        });
    }
});