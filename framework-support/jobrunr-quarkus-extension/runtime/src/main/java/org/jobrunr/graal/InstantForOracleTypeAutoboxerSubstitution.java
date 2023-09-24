package org.jobrunr.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import java.time.Instant;
import java.util.function.BooleanSupplier;

@TargetClass(className = "org.jobrunr.utils.reflection.autobox.InstantForOracleTypeAutoboxer", onlyWith = InstantForOracleTypeAutoboxerSubstitution.IsOracleDatabaseAbsent.class)
public final class InstantForOracleTypeAutoboxerSubstitution {

    @Substitute
    public Instant autobox(Object value, Class<Instant> type) throws Exception {
        throw new UnsupportedOperationException();
    }

    public static class IsOracleDatabaseAbsent implements BooleanSupplier {

        private boolean oracleAbsent;

        public IsOracleDatabaseAbsent() {
            try {
                Class.forName("oracle.sql.TIMESTAMP");
                oracleAbsent = false;
            } catch (ClassNotFoundException e) {
                oracleAbsent = true;
            }
        }

        @Override
        public boolean getAsBoolean() {
            return oracleAbsent;
        }
    }
}
