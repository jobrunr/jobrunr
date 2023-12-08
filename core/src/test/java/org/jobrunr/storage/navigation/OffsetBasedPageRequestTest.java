package org.jobrunr.storage.navigation;

import org.jobrunr.jobs.Job;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.Paging;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class OffsetBasedPageRequestTest {

    @Test
    void testOffsetBasedPageRequestWithEmptyString() {
        OffsetBasedPageRequest offsetBasedPageRequest = OffsetBasedPageRequest.fromString("");

        assertThat(offsetBasedPageRequest).isNull();
    }

    @Test
    void testOffsetBasedPageRequestWithoutLimitAndOffsetFromAndToString() {
        OffsetBasedPageRequest offsetBasedPageRequest = OffsetBasedPageRequest.fromString("order=jobName:ASC&");

        assertThat(offsetBasedPageRequest)
                .hasFieldOrPropertyWithValue("order", "jobName:ASC")
                .hasFieldOrPropertyWithValue("limit", 20)
                .hasFieldOrPropertyWithValue("offset", 0L);

        assertThat(offsetBasedPageRequest.asString()).isEqualTo("order=jobName:ASC&offset=0&limit=20");
    }

    @Test
    void testOffsetBasedPageRequestWithoutOffsetFromAndToString() {
        OffsetBasedPageRequest offsetBasedPageRequest = OffsetBasedPageRequest.fromString("order=jobName:ASC&limit=10");

        assertThat(offsetBasedPageRequest)
                .hasFieldOrPropertyWithValue("order", "jobName:ASC")
                .hasFieldOrPropertyWithValue("limit", 10)
                .hasFieldOrPropertyWithValue("offset", 0L);

        assertThat(offsetBasedPageRequest.asString()).isEqualTo("order=jobName:ASC&offset=0&limit=10");
    }

    @Test
    void testOffsetBasedPageRequestWithEmptyOffsetFromAndToString() {
        OffsetBasedPageRequest offsetBasedPageRequest = OffsetBasedPageRequest.fromString("order=jobName:ASC&limit=10&offset=");

        assertThat(offsetBasedPageRequest)
                .hasFieldOrPropertyWithValue("order", "jobName:ASC")
                .hasFieldOrPropertyWithValue("limit", 10)
                .hasFieldOrPropertyWithValue("offset", 0L);

        assertThat(offsetBasedPageRequest.asString()).isEqualTo("order=jobName:ASC&offset=0&limit=10");
    }

    @Test
    void testOffsetBasedPageRequestWithOffsetFromAndToString() {
        OffsetBasedPageRequest offsetBasedPageRequest = OffsetBasedPageRequest.fromString("order=jobName:ASC&limit=10&offset=20");

        assertThat(offsetBasedPageRequest)
                .hasFieldOrPropertyWithValue("order", "jobName:ASC")
                .hasFieldOrPropertyWithValue("limit", 10)
                .hasFieldOrPropertyWithValue("offset", 20L);

        assertThat(offsetBasedPageRequest.asString()).isEqualTo("order=jobName:ASC&offset=20&limit=10");
    }

    @Test
    void testOffsetBasedPageRequestNextPageWorks() {
        OffsetBasedPageRequest offsetBasedPageRequest = Paging.OffsetBasedPage.ascOnUpdatedAt(2);

        Page<Job> jobPage1 = offsetBasedPageRequest.mapToNewPage(50, asList(
                anEnqueuedJob().withName("Job 1").build(),
                anEnqueuedJob().withName("Job 2").build()));
        OffsetBasedPageRequest page2OffsetBasedPageRequest = Paging.OffsetBasedPage.next(jobPage1);

        Page<Job> jobPage2 = page2OffsetBasedPageRequest.mapToNewPage(50, asList(
                anEnqueuedJob().withName("Job 3").build(),
                anEnqueuedJob().withName("Job 4").build()));

        OffsetBasedPageRequest page1OffsetBasedPageRequest = Paging.OffsetBasedPage.previous(jobPage2);
        OffsetBasedPageRequest page3OffsetBasedPageRequest = Paging.OffsetBasedPage.next(jobPage2);

        assertThat(page1OffsetBasedPageRequest)
                .hasFieldOrPropertyWithValue("order", "jobName:ASC")
                .hasFieldOrPropertyWithValue("offset", 0L)
                .hasFieldOrPropertyWithValue("limit", 2);

        assertThat(page3OffsetBasedPageRequest)
                .hasFieldOrPropertyWithValue("order", "jobName:ASC")
                .hasFieldOrPropertyWithValue("offset", 4L)
                .hasFieldOrPropertyWithValue("limit", 2);
    }
}