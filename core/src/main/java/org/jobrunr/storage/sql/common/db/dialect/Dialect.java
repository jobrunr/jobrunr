package org.jobrunr.storage.sql.common.db.dialect;

public interface Dialect {

    String limitAndOffset(String order);

    String escape(String toEscape);

}
