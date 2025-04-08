package org.jobrunr.tests.e2e;

import org.jobrunr.tests.server.SimpleBackgroundJobServer;
import org.junit.jupiter.api.BeforeAll;

public class DashboardActionsGsonE2ETest extends AbstractDashboardActionsE2ETest {

    @BeforeAll
    static void beforeAll() {
        server = new SimpleBackgroundJobServer()
                .withGsonMapper()
                .withPaused();
        server.start();
    }

}
