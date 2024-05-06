package org.jobrunr.storage.sql.common.db;

import java.util.ArrayList;
import java.util.List;

import static java.sql.Statement.SUCCESS_NO_INFO;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class SqlUtils {

    private SqlUtils() {
    }

    public static <T> List<T> getSucceededItems(List<T> items, int[] insertOrUpdateResult) {
        return range(0, insertOrUpdateResult.length)
                .filter(i -> hasBatchInsertOrUpdateSucceeded(insertOrUpdateResult[i]))
                .mapToObj(items::get)
                .collect(toList());
    }

    public static <T> List<T> getFailedItems(List<T> items, int[] insertOrUpdateResult) {
        List<T> succeededItems = getSucceededItems(items, insertOrUpdateResult);
        List<T> result = new ArrayList<>(items);
        result.removeAll(succeededItems);
        return result;
    }

    private static boolean hasBatchInsertOrUpdateSucceeded(int insertOrUpdateResult) {
        return insertOrUpdateResult == SUCCESS_NO_INFO || insertOrUpdateResult > 0;
    }
}
