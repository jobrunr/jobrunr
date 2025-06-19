package org.jobrunr.tests.e2e;

import org.jobrunr.tests.server.SimpleBackgroundJobServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

@Disabled("for v7")
public class DashboardActionsJsonBE2ETest extends AbstractDashboardActionsE2ETest {

    @BeforeAll
    static void beforeAll() {
        server = new SimpleBackgroundJobServer()
                .withJsonBMapper()
                .withPaused();
        server.start();
    }

}
