package org.jobrunr.jobs.details;

public class UnsupportedLambdaException extends IllegalArgumentException {

    public static final String SUM = "You are summing two numbers while enqueueing/scheduling jobs - for performance reasons it is better to do the calculation outside of the job lambda";
    public static final String SUBTRACTION = "You are subtracting two numbers while enqueueing/scheduling jobs - for performance reasons it is better to do the calculation outside of the job lambda";
    public static final String MULTIPLICATION = "You are multiplying two numbers while enqueueing/scheduling jobs - for performance reasons it is better to do the calculation outside of the job lambda";
    public static final String DIVISION = "You are dividing two numbers while enqueueing/scheduling jobs - for performance reasons it is better to do the calculation outside of the job lambda";
    public static final String REMAINDER = "You are calculating the remainder (%) for two numbers while enqueueing/scheduling jobs - for performance reasons it is better to do the calculation outside of the job lambda";

    public static final String INCREMENT = "You are changing a local variable (e.g. i++, i-- or i += 1) while enqueueing/scheduling jobs - JobRunr cannot determine the job parameters, so do the calculation outside of the job lambda";
    public static final String JUMP = "You are using an if/else, a loop or a short-circuit operator (&&, ||) while enqueueing/scheduling jobs - JobRunr cannot determine the job parameters, so do the calculation outside of the job lambda";
    public static final String SWITCH = "You are using a switch statement while enqueueing/scheduling jobs - JobRunr cannot determine the job parameters, so do the calculation outside of the job lambda";

    public UnsupportedLambdaException(String message) {
        super(message);
    }
}
