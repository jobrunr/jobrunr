package org.jobrunr.jobs.details;

import org.jobrunr.utils.GraalVMUtils;
import org.jobrunr.utils.VersionNumber;

import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;
import static org.jobrunr.utils.reflection.ReflectionUtils.loadClass;

public class JobDetailsGeneratorFactory {

    private static final String CLASS_FILE_API_JOB_DETAILS_GENERATOR = "org.jobrunr.jobs.details.ClassFileApiJobDetailsGenerator";

    private JobDetailsGeneratorFactory() {
    }

    public static JobDetailsGenerator createDefault() {
        return isClassFileApiJobDetailsGeneratorAvailable()
                ? newClassFileApiJobDetailsGenerator()
                : new JobDetailsAsmGenerator();
    }

    private static boolean isClassFileApiJobDetailsGeneratorAvailable() {
        return VersionNumber.JAVA_VERSION.hasMajorVersionHigherOrEqualTo(25)
                && !GraalVMUtils.isRunningInGraalVMNativeMode()
                && classExists(CLASS_FILE_API_JOB_DETAILS_GENERATOR);
    }

    private static JobDetailsGenerator newClassFileApiJobDetailsGenerator() {
        try {
            return (JobDetailsGenerator) loadClass(CLASS_FILE_API_JOB_DETAILS_GENERATOR).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw shouldNotHappenException(e);
        }
    }
}
