package org.assertj.db;

public interface DatabaseQueries {

    String getAllTablesQuery();

    String getAllViewsQuery();

    String getAllIndicesQuery();
}
