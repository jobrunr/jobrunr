package org.jobrunr.storage.sql.common.db;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.storage.sql.common.db.ConcurrentSqlModificationException.concurrentDatabaseModificationException;

class ConcurrentSqlModificationExceptionTest {

    @Test
    void getFailedItemsReturnsObjectsThatCouldNotBeSaved() {
        Job job1 = aJobInProgress().build();
        Job job2 = aJobInProgress().build();
        Job job3 = aJobInProgress().build();
        Job job4 = aJobInProgress().build();
        Job job5 = aJobInProgress().build();
        Job job6 = aJobInProgress().build();
        Job job7 = aJobInProgress().build();

        final ConcurrentSqlModificationException concurrentSqlModificationException = concurrentDatabaseModificationException(
                asList(job1, job2, job3, job4, job5, job6, job7),
                new int[]{1, -3, -3, -2, 1, 0, -3}
        );

        assertThat(concurrentSqlModificationException.getFailedItems()).containsExactly(job2, job3, job6, job7);
    }

}