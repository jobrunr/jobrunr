package org.jobrunr.server.costaware;

import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.BackgroundJobServerStatusMetadata;

import java.math.BigDecimal;
import java.time.Duration;
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


    public void save(List<BackgroundJobServerStatus> backgroundJobServers, Duration pollInterval) {
        Instant now = Instant.now();
        ZonedDateTime zonedDateTime = now.atZone(ZoneId.systemDefault());
        LocalDate currentDate = LocalDate.from(zonedDateTime);
        YearMonth currentMonth = YearMonth.from(zonedDateTime);
        Year currentYear = Year.from(zonedDateTime);

        for (BackgroundJobServerStatus backgroundJobServer : backgroundJobServers) {
            updateBackgroundJobServerSavings(backgroundJobServer);
        }

        calculateDailySavings(currentDate, pollInterval);
        calculateMonthlySavings(currentMonth);
        calculateYearlySavings(currentYear);
    }

    private void updateBackgroundJobServerSavings(BackgroundJobServerStatus backgroundJobServer) {
        if (backgroundJobServer.getMetadata() == null || backgroundJobServer.getMetadata().getSpotPrice() == null) return;

        BackgroundJobServerSavings savings = backgroundJobServerSavings.get(backgroundJobServer.getId());
        BackgroundJobServerStatusMetadata metadata = backgroundJobServer.getMetadata();
        if (savings == null) {
            savings = new BackgroundJobServerSavings(
                    backgroundJobServer.getId(),
                    backgroundJobServer.getFirstHeartbeat(),
                    backgroundJobServer.getLastHeartbeat(),
                    metadata == null ? BigDecimal.ZERO : metadata.getServerSavings(),
                    metadata == null ? BigDecimal.ZERO : metadata.getSpotPrice(),
                    metadata == null ? BigDecimal.ZERO : metadata.getInstancePrice()
            );
        } else {
            if (metadata != null) {
                savings.updateSavings(backgroundJobServer);
            }
        }
        backgroundJobServerSavings.put(backgroundJobServer.getId(), savings);
    }

    private void calculateDailySavings(LocalDate currentDate, Duration pollInterval) {
        Savings savings = dailySavings.get(currentDate);
        if (savings == null) {
            savings = new DailySavings(currentDate);
        }
        Instant lastHeartbeatMustBeAfter = Instant.now().minus(pollInterval.multipliedBy(4));
        DailySavings dailySaving = savings.toDailySavings();
        backgroundJobServerSavings.values().stream()
                .filter(saving -> LocalDate.from(saving.removedAt.atZone(ZoneId.systemDefault())).equals(currentDate)) // Why? Make sure that data is from today
                .filter(saving -> saving.removedAt.isAfter(lastHeartbeatMustBeAfter)) // Why? If it's been updated more than 4 poll intervals ago, filter it out
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

    public HashMap<LocalDate, Savings> getDailySavings() {
        return dailySavings;
    }

    public HashMap<YearMonth, Savings> getMonthlySavings() {
        return monthlySavings;
    }

    public HashMap<Year, Savings> getYearlySavings() {
        return yearlySavings;
    }

    public static class BackgroundJobServerSavings {
        private final UUID serverId;
        private final Instant createdAt;
        private final BigDecimal spotPrice;
        private final BigDecimal instancePrice;
        private Instant removedAt;
        private BigDecimal totalSavings;
        private BigDecimal lastIncreasedBy;
        private BigDecimal spotSpendIncrease;
        private BigDecimal instanceSpendIncrease;

        public BackgroundJobServerSavings(
                UUID serverId,
                Instant createdAt,
                Instant removedAt,
                BigDecimal totalSavings,
                BigDecimal spotPrice,
                BigDecimal instancePrice
        ) {
            this.serverId = serverId;
            this.createdAt = createdAt;
            this.removedAt = removedAt;
            this.totalSavings = totalSavings;
            this.lastIncreasedBy = totalSavings;
            this.spotPrice = spotPrice;
            this.instancePrice = instancePrice;
            this.spotSpendIncrease = BigDecimal.ZERO;
            this.instanceSpendIncrease = BigDecimal.ZERO;
        }

        public void updateSavings(BackgroundJobServerStatus backgroundJobServerStatus) {
            Instant now = Instant.now();
            Duration timeSinceLastUpdate = Duration.between(removedAt, now);
            this.removedAt = now;
            this.lastIncreasedBy = backgroundJobServerStatus.getMetadata().getServerSavings().subtract(this.totalSavings);
            this.totalSavings = backgroundJobServerStatus.getMetadata().getServerSavings();
            this.spotSpendIncrease = spotPrice.multiply(BigDecimal.valueOf(timeSinceLastUpdate.toMinutes() / 60.0));
            this.instanceSpendIncrease = instancePrice.multiply(BigDecimal.valueOf(timeSinceLastUpdate.toMinutes() / 60.0));
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

        public BigDecimal getSpotSpendIncrease() {
            return spotSpendIncrease;
        }

        public BigDecimal getInstanceSpendIncrease() {
            return instanceSpendIncrease;
        }
    }

    public static abstract class Savings {
        private final ChronoUnit chronoUnit;
        private final Temporal period;
        protected BigDecimal totalSavings;
        protected BigDecimal spotInstanceSpend;
        protected BigDecimal equivalentInstanceSpend;

        Savings(ChronoUnit chronoUnit, Temporal period) {
            this(chronoUnit, period, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        public Savings(
                ChronoUnit chronoUnit,
                Temporal period,
                BigDecimal totalSavings,
                BigDecimal spotInstanceSpend,
                BigDecimal equivalentInstanceSpend
        ) {
            this.chronoUnit = chronoUnit;
            this.period = period;
            this.totalSavings = totalSavings;
            this.spotInstanceSpend = spotInstanceSpend;
            this.equivalentInstanceSpend = equivalentInstanceSpend;
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

        public BigDecimal getSpotInstanceSpend() {
            return spotInstanceSpend;
        }

        public BigDecimal getEquivalentInstanceSpend() {
            return equivalentInstanceSpend;
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

        public DailySavings(ChronoUnit chronoUnit, Temporal period, BigDecimal totalSavings, BigDecimal spotInstanceSpend, BigDecimal equivalentInstanceSpend) {
            super(chronoUnit, period, totalSavings, spotInstanceSpend, equivalentInstanceSpend);
        }

        public void addBackgroundJobServerSavings(BackgroundJobServerSavings backgroundJobServerSavings) {
            totalSavings = totalSavings.add(backgroundJobServerSavings.lastIncreasedBy);
            spotInstanceSpend = spotInstanceSpend.add(backgroundJobServerSavings.spotSpendIncrease);
            equivalentInstanceSpend = equivalentInstanceSpend.add(backgroundJobServerSavings.instanceSpendIncrease);
        }
    }

    public static class MonthlySavings extends Savings {
        public MonthlySavings(ChronoUnit chronoUnit, Temporal period, BigDecimal totalSavings, BigDecimal spotInstanceSpend, BigDecimal equivalentInstanceSpend) {
            super(chronoUnit, period, totalSavings, spotInstanceSpend, equivalentInstanceSpend);
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
        public YearlySavings(ChronoUnit chronoUnit, Temporal period, BigDecimal totalSavings, BigDecimal spotInstanceSpend, BigDecimal equivalentInstanceSpend) {
            super(chronoUnit, period, totalSavings, spotInstanceSpend, equivalentInstanceSpend);
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
