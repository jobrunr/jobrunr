package org.jobrunr.scheduling.cron;

import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;

class CronFieldParser {

    private static final String INVALID_FIELD = "invalid %s field: \"%s\".";

    private static Map<String, Integer> MONTHS_NAMES;
    private static Map<String, Integer> DAYS_OF_WEEK_NAMES;

    static {
        CronFieldParser.MONTHS_NAMES = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        CronFieldParser.MONTHS_NAMES.put("January", 1);
        CronFieldParser.MONTHS_NAMES.put("Jan", 1);
        CronFieldParser.MONTHS_NAMES.put("February", 2);
        CronFieldParser.MONTHS_NAMES.put("Feb", 2);
        CronFieldParser.MONTHS_NAMES.put("March", 3);
        CronFieldParser.MONTHS_NAMES.put("Mar", 3);
        CronFieldParser.MONTHS_NAMES.put("April", 4);
        CronFieldParser.MONTHS_NAMES.put("Apr", 4);
        CronFieldParser.MONTHS_NAMES.put("May", 5);
        CronFieldParser.MONTHS_NAMES.put("June", 6);
        CronFieldParser.MONTHS_NAMES.put("Jun", 6);
        CronFieldParser.MONTHS_NAMES.put("July", 7);
        CronFieldParser.MONTHS_NAMES.put("Jul", 7);
        CronFieldParser.MONTHS_NAMES.put("August", 8);
        CronFieldParser.MONTHS_NAMES.put("Aug", 8);
        CronFieldParser.MONTHS_NAMES.put("September", 9);
        CronFieldParser.MONTHS_NAMES.put("Sep", 9);
        CronFieldParser.MONTHS_NAMES.put("October", 10);
        CronFieldParser.MONTHS_NAMES.put("Oct", 10);
        CronFieldParser.MONTHS_NAMES.put("November", 11);
        CronFieldParser.MONTHS_NAMES.put("Nov", 11);
        CronFieldParser.MONTHS_NAMES.put("December", 12);
        CronFieldParser.MONTHS_NAMES.put("Dec", 12);

        CronFieldParser.DAYS_OF_WEEK_NAMES = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Sunday", 0);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Sun", 0);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Monday", 1);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Mon", 1);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Tuesday", 2);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Tue", 2);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Wednesday", 3);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Wed", 3);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Thursday", 4);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Thu", 4);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Friday", 5);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Fri", 5);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("Saturday", 6);
        CronFieldParser.DAYS_OF_WEEK_NAMES.put("sat", 6);
    }

    private final CronFieldType fieldType;
    private final int length;
    private final int maxAllowedValue;
    private final int minAllowedValue;
    private final String fieldName;

    CronFieldParser(CronFieldType fieldType) {
        this.fieldType = fieldType;
        this.fieldName = fieldType.getFieldName();
        this.length = fieldType.getLength();
        this.maxAllowedValue = fieldType.getMaxAllowedValue();
        this.minAllowedValue = fieldType.getMinAllowedValue();
    }

    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private int parseValue(String token) {
        return Integer.parseInt(token);
    }

    public BitSet parse(String token) {
        if (token.indexOf(',') > -1) {
            BitSet bitSet = new BitSet(this.length);
            String[] items = token.split(",");
            for (String item : items) {
                bitSet.or(this.parse(item));
            }
            return bitSet;
        }

        if (token.indexOf('/') > -1)
            return this.parseStep(token);

        if (token.indexOf('-') > -1)
            return this.parseRange(token);

        if (token.equalsIgnoreCase("*")) {
            return fieldType.parseAsterisk();
        }

        return this.parseLiteral(token);
    }

    private BitSet parseStep(String token) {
        try {
            String[] tokenParts = token.split("/");
            if (tokenParts.length != 2) {
                throw new InvalidCronExpressionException(String.format(INVALID_FIELD, this.fieldName, token));
            }
            String stepSizePart = tokenParts[1];
            int stepSize = this.parseValue(stepSizePart);
            if (stepSize < 1) {
                throw new InvalidCronExpressionException(String.format(INVALID_FIELD + " minimum allowed step (every) value is \"1\"", this.fieldName, token));
            }
            String numSetPart = tokenParts[0];
            if (!numSetPart.contains("-") && !numSetPart.equals("*") && isInteger(numSetPart)) {
                // if number is a single digit, it should be a range starts with that
                // number and ends with the maximum allowed value for the field type
                numSetPart = String.format("%s-%d", numSetPart, this.maxAllowedValue);
            }
            BitSet numSet = this.parse(numSetPart);
            BitSet stepsSet = new BitSet(this.length);
            for (int i = numSet.nextSetBit(0); i < this.length; i += stepSize) {
                stepsSet.set(i);
            }
            stepsSet.and(numSet);
            return stepsSet;
        } catch (NumberFormatException ex) {
            throw new InvalidCronExpressionException(String.format(INVALID_FIELD, this.fieldName, token), ex);
        }
    }

    private BitSet parseRange(String token) {
        String[] rangeParts = token.split("-");
        if (rangeParts.length != 2) {
            throw new InvalidCronExpressionException(String.format(INVALID_FIELD, this.fieldName, token));
        }
        try {
            int from = this.parseValue(rangeParts[0]);
            if (from < 0) {
                throw new InvalidCronExpressionException(String.format(INVALID_FIELD, this.fieldName, token));
            }
            if (from < this.minAllowedValue) {
                throw new InvalidCronExpressionException(
                        String.format(INVALID_FIELD + " minimum allowed value for %s field is \"%d\"",
                                this.fieldName, token, this.fieldName, this.minAllowedValue));
            }

            int to = this.parseValue(rangeParts[1]);
            if (to < 0) {
                throw new InvalidCronExpressionException(
                        String.format(INVALID_FIELD, this.fieldName, token));
            }
            if (to > this.maxAllowedValue) {
                throw new InvalidCronExpressionException(
                        String.format(INVALID_FIELD + " maximum allowed value for %s field is \"%d\"",
                                this.fieldName, token, this.fieldName, this.maxAllowedValue));
            }
            if (to < from) {
                throw new InvalidCronExpressionException(String.format(
                        INVALID_FIELD + " the start of range value must be less than or equal the end value",
                        this.fieldName, token));
            }
            return fieldType.fillBitSetToIncl(from, to);
        } catch (NumberFormatException ex) {
            throw new InvalidCronExpressionException(String.format(INVALID_FIELD, this.fieldName, token), ex);
        }
    }

    private BitSet parseLiteral(String token) {
        BitSet bitSet = new BitSet(this.length);
        try {
            int number = this.parseValue(token);
            if (number < 0) {
                throw new InvalidCronExpressionException(String.format(INVALID_FIELD, this.fieldName, token));
            }
            if (number < this.minAllowedValue) {
                throw new InvalidCronExpressionException(
                        String.format(INVALID_FIELD + " minimum allowed value for %s field is \"%d\"",
                                this.fieldName, token, this.fieldName, this.minAllowedValue));
            }
            if (number > this.maxAllowedValue) {
                throw new InvalidCronExpressionException(
                        String.format(INVALID_FIELD + " maximum allowed value for %s field is \"%d\"",
                                this.fieldName, token, this.fieldName, this.maxAllowedValue));
            }
            fieldType.setBitSet(bitSet, number);
        } catch (NumberFormatException ex) {
            throw new InvalidCronExpressionException(String.format(INVALID_FIELD, this.fieldName, token), ex);
        }
        return bitSet;
    }
}
