package org.jobrunr.server.costaware;

import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.BackgroundJobServerStatusMetadata;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CostAwareTotalSavings {

    private final HashMap<UUID, BackgroundJobServerSavings> backgroundJobServerSavings;
    private final HashMap<LocalDate, Savings> dailySavings; // last 31 days
    private final HashMap<YearMonth, Savings> monthlySavings;
    private final HashMap<Year, Savings> yearlySavings;

    public CostAwareTotalSavings() {
        backgroundJobServerSavings = new HashMap<>();
        dailySavings = new HashMap<>();
        monthlySavings = new HashMap<>();
        yearlySavings = new HashMap<>();
    }

    public CostAwareTotalSavings(
            HashMap<UUID, BackgroundJobServerSavings> backgroundJobServerSavings,
            HashMap<LocalDate, Savings> dailySavings,
            HashMap<YearMonth, Savings> monthlySavings,
            HashMap<Year, Savings> yearlySavings
    ) {
        this.backgroundJobServerSavings = backgroundJobServerSavings;
        this.dailySavings = dailySavings;
        this.monthlySavings = monthlySavings;
        this.yearlySavings = yearlySavings;
    }


    public void save(List<BackgroundJobServerStatus> backgroundJobServers) {
        Instant now = Instant.now();
        ZonedDateTime zonedDateTime = now.atZone(ZoneId.systemDefault());
        LocalDate currentDate = LocalDate.from(zonedDateTime);
        YearMonth currentMonth = YearMonth.from(zonedDateTime);
        Year currentYear = Year.from(zonedDateTime);

        for (BackgroundJobServerStatus backgroundJobServer : backgroundJobServers) {
            updateBackgroundJobServerSavings(backgroundJobServer);
        }

        calculateDailySavings(currentDate);
        calculateMonthlySavings(currentMonth);
        calculateYearlySavings(currentYear);
    }

    private void updateBackgroundJobServerSavings(BackgroundJobServerStatus backgroundJobServer) {
        if (backgroundJobServer.getMetadata() == null || backgroundJobServer.getMetadata().getSpotPrice() == null) return;

        BackgroundJobServerSavings savings = backgroundJobServerSavings.get(backgroundJobServer.getId());
        BackgroundJobServerStatusMetadata metadata = backgroundJobServer.getMetadata();
        if (savings == null) {
            savings = new BackgroundJobServerSavings(backgroundJobServer.getId(), backgroundJobServer.getFirstHeartbeat(), backgroundJobServer.getLastHeartbeat(), metadata == null ? BigDecimal.ZERO : metadata.getServerSavings());
        } else {
            if (metadata != null) {
                savings.updateSavings(backgroundJobServer);
            }
        }
        backgroundJobServerSavings.put(backgroundJobServer.getId(), savings);
    }

    private void calculateDailySavings(LocalDate currentDate) {
        Savings savings = dailySavings.get(currentDate);
        if (savings == null) {
            savings = new DailySavings(currentDate);
        }
        DailySavings dailySaving = savings.toDailySavings();
        backgroundJobServerSavings.values().stream()
                .filter(saving -> LocalDate.from(saving.removedAt.atZone(ZoneId.systemDefault())).equals(currentDate))
                .forEach(dailySaving::addBackgroundJobServerSavings);
        dailySavings.put(currentDate, dailySaving);
    }

    private void calculateMonthlySavings(YearMonth currentMonth) {
        if (dailySavings.size() >= 28) {
            monthlySavings.computeIfAbsent(currentMonth.minus(1, ChronoUnit.MONTHS), k -> new MonthlySavings(k, new ArrayList<>(dailySavings.values().stream().map(Savings::toDailySavings).toList())));
        }
        if (dailySavings.size() > 31) {
            LocalDate earliestDate = dailySavings.keySet().stream().min(LocalDate::compareTo).orElseThrow();
            dailySavings.remove(earliestDate);
        }
    }

    private void calculateYearlySavings(Year currentYear) {
        if (monthlySavings.size() >= 12) {
            yearlySavings.computeIfAbsent(currentYear.minus(1, ChronoUnit.YEARS), k -> new YearlySavings(k, new ArrayList<>(monthlySavings.values().stream().map(Savings::toMonthlySavings).toList())));
        }
        if (monthlySavings.size() > 12) {
            YearMonth earliestYear = monthlySavings.keySet().stream().min(YearMonth::compareTo).orElseThrow();
            monthlySavings.remove(earliestYear);
        }
    }

    public static class BackgroundJobServerSavings {
        private final UUID serverId;
        private final Instant createdAt;
        private Instant removedAt;
        private BigDecimal totalSavings;
        private BigDecimal lastIncreasedBy;

        public BackgroundJobServerSavings(
                UUID serverId,
                Instant createdAt,
                Instant removedAt,
                BigDecimal totalSavings
        ) {
            this.serverId = serverId;
            this.createdAt = createdAt;
            this.removedAt = removedAt;
            this.totalSavings = totalSavings;
            this.lastIncreasedBy = totalSavings;
        }

        public void updateSavings(BackgroundJobServerStatus backgroundJobServerStatus) {
            this.removedAt = Instant.now();
            this.lastIncreasedBy = backgroundJobServerStatus.getMetadata().getServerSavings().subtract(this.totalSavings);
            this.totalSavings = backgroundJobServerStatus.getMetadata().getServerSavings();
        }

        public UUID getServerId() {
            return serverId;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public Instant getRemovedAt() {
            return removedAt;
        }

        public BigDecimal getTotalSavings() {
            return totalSavings;
        }

        public BigDecimal getLastIncreasedBy() {
            return lastIncreasedBy;
        }
    }

    public static abstract class Savings {
        private final ChronoUnit chronoUnit;
        private final Temporal period;
        protected BigDecimal totalSavings;

        Savings(ChronoUnit chronoUnit, Temporal period) {
            this(chronoUnit, period, BigDecimal.ZERO);
        }

        public Savings(
                ChronoUnit chronoUnit,
                Temporal period,
                BigDecimal totalSavings
        ) {
            this.chronoUnit = chronoUnit;
            this.period = period;
            this.totalSavings = totalSavings;
        }

        public ChronoUnit getChronoUnit() {
            return chronoUnit;
        }

        public Temporal getPeriod() {
            return period;
        }

        public BigDecimal getTotalSavings() {
            return totalSavings;
        }

        public boolean isDaily() {
            return chronoUnit.equals(ChronoUnit.DAYS);
        }

        public boolean isMonthly() {
            return chronoUnit.equals(ChronoUnit.MONTHS);
        }

        public boolean isYearly() {
            return chronoUnit.equals(ChronoUnit.YEARS);
        }

        public DailySavings toDailySavings() {
            if (isDaily()) return (DailySavings) this;
            else return null;
        }

        public MonthlySavings toMonthlySavings() {
            if (isMonthly()) return (MonthlySavings) this;
            else return null;
        }

        public YearlySavings toYearlySavings() {
            if (isYearly()) return (YearlySavings) this;
            else return null;
        }
    }

    public static class DailySavings extends Savings {

        public DailySavings(LocalDate start) {
            super(ChronoUnit.DAYS, start);
        }

        public DailySavings(ChronoUnit chronoUnit, Temporal period, BigDecimal totalSavings) {
            super(chronoUnit, period, totalSavings);
        }

        public void addBackgroundJobServerSavings(BackgroundJobServerSavings backgroundJobServerSavings) {
            totalSavings = totalSavings.add(backgroundJobServerSavings.lastIncreasedBy);
        }
    }

    public static class MonthlySavings extends Savings {
        public MonthlySavings(ChronoUnit chronoUnit, Temporal period, BigDecimal totalSavings) {
            super(chronoUnit, period, totalSavings);
        }

        public MonthlySavings(YearMonth yearMonth, List<DailySavings> dailySavings) {
            super(ChronoUnit.MONTHS, yearMonth);
            totalSavings = dailySavings.stream()
                    .filter(saving -> getPeriod().equals(yearMonth))
                    .map(saving -> saving.totalSavings)
                    .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        }
    }

    public static class YearlySavings extends Savings {
        public YearlySavings(ChronoUnit chronoUnit, Temporal period, BigDecimal totalSavings) {
            super(chronoUnit, period, totalSavings);
        }

        public YearlySavings(Year year, List<MonthlySavings> monthlySavings) {
            super(ChronoUnit.YEARS, year);
            totalSavings = monthlySavings.stream()
                    .filter(saving -> getPeriod().equals(year))
                    .map(saving -> saving.totalSavings)
                    .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        }
    }
}
