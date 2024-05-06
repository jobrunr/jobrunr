package org.jobrunr.storage.sql.common.db;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.sql.Statement.EXECUTE_FAILED;
import static java.sql.Statement.SUCCESS_NO_INFO;
import static java.util.Arrays.asList;
import static org.jobrunr.JobRunrAssertions.assertThatJobs;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;

class SqlUtilsTest {

    @Test
    void testSucceededAndFailedItems() {
        // GIVEN
        Job job1 = aJobInProgress().build();
        Job job2 = aJobInProgress().build();
        Job job3 = aJobInProgress().build();
        Job job4 = aJobInProgress().build();
        Job job5 = aJobInProgress().build();
        List<Job> items = asList(job1, job2, job3, job4, job5);

        // WHEN & THEN
        List<Job> succeededItems = SqlUtils.getSucceededItems(items, new int[]{EXECUTE_FAILED, 1, SUCCESS_NO_INFO, EXECUTE_FAILED, 1});
        assertThatJobs(succeededItems)
                .hasSize(3)
                .contains(job2, job3, job5);

        // WHEN & THEN
        List<Job> failedItems = SqlUtils.getFailedItems(items, new int[]{EXECUTE_FAILED, 1, 1, EXECUTE_FAILED, 1});
        assertThatJobs(failedItems)
                .hasSize(2)
                .contains(job1, job4);
    }

    @Test
    void testOracleA() {
        // GIVEN
        Job job1 = aJobInProgress().build();
        Job job2 = aJobInProgress().build();
        Job job3 = aJobInProgress().build();
        Job job4 = aJobInProgress().build();
        List<Job> items = asList(job1, job2, job3, job4);

        // WHEN & THEN
        List<Job> succeededItems = SqlUtils.getSucceededItems(items, new int[0]);
        assertThatJobs(succeededItems).isEmpty();

        // WHEN & THEN
        List<Job> failedItems = SqlUtils.getFailedItems(items, new int[0]);
        assertThatJobs(failedItems)
                .hasSize(4)
                .contains(job1, job2, job3, job4);
    }

    @Test
    void testOracleB() {
        // GIVEN
        Job job1 = aJobInProgress().build();
        Job job2 = aJobInProgress().build();
        Job job3 = aJobInProgress().build();
        Job job4 = aJobInProgress().build();
        Job job5 = aJobInProgress().build();
        Job job6 = aJobInProgress().build();
        List<Job> items = asList(job1, job2, job3, job4, job5, job6);

        // WHEN & THEN
        List<Job> succeededItems = SqlUtils.getSucceededItems(items, new int[]{1, 1, 1, 1});
        assertThatJobs(succeededItems)
                .hasSize(4)
                .containsExactly(job1, job2, job3, job4);

        // WHEN & THEN
        List<Job> failedItems = SqlUtils.getFailedItems(items, new int[]{1, 1, 1, 1});
        assertThatJobs(failedItems)
                .hasSize(2)
                .contains(job5, job6);
    }
}