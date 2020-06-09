package org.jobrunr.storage.sql.common.db;

import org.jobrunr.storage.StorageException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.sql.Statement.SUCCESS_NO_INFO;
import static java.util.Collections.singletonList;

public class ConcurrentSqlModificationException extends StorageException {

    private final List<?> items;
    private final int[] insertOrUpdateResult;

    public static ConcurrentSqlModificationException concurrentDatabaseModificationException(Object item, int insertOrUpdateResult) {
        return concurrentDatabaseModificationException(singletonList(item), new int[]{insertOrUpdateResult});
    }

    public static ConcurrentSqlModificationException concurrentDatabaseModificationException(List<?> items, int[] insertOrUpdateResult) {
        return new ConcurrentSqlModificationException("Could not insert or update all objects - not all updates succeeded: " + Arrays.toString(insertOrUpdateResult), items, insertOrUpdateResult);
    }

    private ConcurrentSqlModificationException(String reason, List<?> items, int[] insertOrUpdateResult) {
        super(reason);
        this.items = items;
        this.insertOrUpdateResult = insertOrUpdateResult;
    }

    public List<Object> getFailedItems() {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < insertOrUpdateResult.length; i++) {
            if (insertOrUpdateResult[i] < SUCCESS_NO_INFO || insertOrUpdateResult[i] == 0) {
                result.add(items.get(i));
            }
        }
        return result;
    }
}
