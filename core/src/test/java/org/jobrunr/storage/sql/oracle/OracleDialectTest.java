package org.jobrunr.storage.sql.oracle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OracleDialectTest {

    OracleDialect dialect = new OracleDialect();

    @Test
    void testWithSelectForUpdate() {
        String statement = "select jobAsJson from jobrunr_jobs j where state = :state AND serverTag in ('DEFAULT') AND (j.mutex is null or j.mutex not in (select distinct mutexInUse from jobrunr_jobs where mutexInUse is not null)) " + dialect.limit() + dialect.selectForUpdateSkipLocked();

        assertThat(dialect.escape(statement))
                .isEqualTo("select jobAsJson from jobrunr_jobs j where ROWNUM <= :limit and state = :state AND serverTag in ('DEFAULT') AND (j.mutex is null or j.mutex not in (select distinct mutexInUse from jobrunr_jobs where mutexInUse is not null)) FOR UPDATE SKIP LOCKED");
    }

    @Test
    void testWithoutSelectForUpdate() {
        String statement = "select jobAsJson from jobrunr_jobs j where state = :state AND serverTag in ('DEFAULT') AND (j.mutex is null or j.mutex not in (select distinct mutexInUse from jobrunr_jobs where mutexInUse is not null)) " + dialect.limit();

        assertThat(dialect.escape(statement))
                .isEqualTo("select jobAsJson from jobrunr_jobs j where state = :state AND serverTag in ('DEFAULT') AND (j.mutex is null or j.mutex not in (select distinct mutexInUse from jobrunr_jobs where mutexInUse is not null)) FETCH NEXT :limit ROWS ONLY");
    }
}