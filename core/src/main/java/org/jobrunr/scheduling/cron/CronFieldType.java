package org.jobrunr.scheduling.cron;

import java.util.BitSet;

public enum CronFieldType {
    SECOND(60, 0, 59),
    MINUTE(60, 0, 59),
    HOUR(24, 0, 23),
    DAY(31, 1, 31),
    MONTH(12, 1, 12),
    DAY_OF_WEEK(7, 0, 6);

    private final int length;
    private final int minAllowedValue;
    private final int maxAllowedValue;

    CronFieldType(int length, int minAllowedValue, int maxAllowedValue) {
        this.length = length;
        this.minAllowedValue = minAllowedValue;
        this.maxAllowedValue = maxAllowedValue;
    }

    public String getFieldName() {
        return this.name().toLowerCase();
    }

    public int getLength() {
        return length;
    }

    public int getMinAllowedValue() {
        return minAllowedValue;
    }

    public int getMaxAllowedValue() {
        return maxAllowedValue;
    }

    public BitSet parseAsterisk() {
        if (this == MONTH) {
            return fillBitSet(1, length + 1);
        }
        return fillBitSet(0, length);
    }

    public BitSet parseLastDayOfMonth() {
        if (this == DAY) {
            BitSet bitSet = fillBitSet(27, length);
            return bitSet;
        }
        throw new InvalidCronExpressionException("Last day of month is only allowed in day field");
    }

    public BitSet fillBitSetToIncl(int from, int toIncluded) {
        int fromIndex = from - minAllowedValue;
        int toIndex = toIncluded - minAllowedValue + 1;
        if (this == MONTH) {
            fromIndex = from;
            toIndex = toIncluded + 1;
        }
        return fillBitSet(fromIndex, toIndex);
    }

    public BitSet fillBitSet(int from, int toExcluded) {
        BitSet bitSet = new BitSet(toExcluded);
        bitSet.set(from, toExcluded);
        return bitSet;
    }

    public void setBitSet(BitSet bitSet, int number) {
        bitSet.set(this == MONTH ? number : number - this.minAllowedValue);
    }
}
