package org.jobrunr.storage.nosql.mongo;

import java.util.Objects;

public class MongoCollectionPrefixProcessor {
    private final String tablePrefix;

    public MongoCollectionPrefixProcessor(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public String applyCollectionPrefix(String collectionName) {
        return this.tablePrefix + collectionName;
    }

    @Override
    public String toString() {
        return "MongoTablePrefixProcessor{" +
                "tablePrefix='" + tablePrefix + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MongoCollectionPrefixProcessor that = (MongoCollectionPrefixProcessor) o;
        return tablePrefix.equals(that.tablePrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tablePrefix);
    }
}
