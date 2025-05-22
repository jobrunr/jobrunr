package org.jobrunr.storage;

import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;

public class Paging {

    private Paging() {
    } // for sonarqube

    public static class AmountBasedList {
        public static AmountRequest ascOnUpdatedAt(int amount) {
            return new AmountRequest(StorageProviderUtils.Jobs.FIELD_UPDATED_AT + ":ASC", amount);
        }

        public static AmountRequest descOnUpdatedAt(int amount) {
            return new AmountRequest(StorageProviderUtils.Jobs.FIELD_UPDATED_AT + ":DESC", amount);
        }

        public static AmountRequest ascOnCreatedAt(int amount) {
            return new AmountRequest(StorageProviderUtils.Jobs.FIELD_CREATED_AT + ":ASC", amount);
        }

        public static AmountRequest ascOnScheduledAt(int amount) {
            return new AmountRequest(StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT + ":ASC", amount);
        }

        public static AmountRequest ascOnCarbonAwareDeadline(int amount) {
            return new AmountRequest(StorageProviderUtils.Jobs.FIELD_DEADLINE + ":ASC", amount);
        }
    }

    public static class OffsetBasedPage {

        public static OffsetBasedPageRequest next(Page page) {
            return OffsetBasedPageRequest.fromString(page.getNextPageRequest());
        }

        public static OffsetBasedPageRequest previous(Page page) {
            return OffsetBasedPageRequest.fromString(page.getPreviousPageRequest());
        }

        public static OffsetBasedPageRequest ascOnUpdatedAt(int amount) {
            return ascOnUpdatedAt(0, amount);
        }

        public static OffsetBasedPageRequest ascOnUpdatedAt(int offset, int amount) {
            return new OffsetBasedPageRequest(StorageProviderUtils.Jobs.FIELD_UPDATED_AT + ":ASC", offset, amount);
        }

        public static OffsetBasedPageRequest descOnUpdatedAt(int amount) {
            return descOnUpdatedAt(0, amount);
        }

        public static OffsetBasedPageRequest descOnUpdatedAt(int offset, int amount) {
            return new OffsetBasedPageRequest(StorageProviderUtils.Jobs.FIELD_UPDATED_AT + ":DESC", offset, amount);
        }

        public static OffsetBasedPageRequest ascOnScheduledAt(int amount) {
            return ascOnScheduledAt(0, amount);
        }

        public static OffsetBasedPageRequest ascOnScheduledAt(int offset, int amount) {
            return new OffsetBasedPageRequest(StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT + ":ASC", offset, amount);
        }
    }
}