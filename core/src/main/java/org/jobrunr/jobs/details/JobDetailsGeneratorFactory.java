package org.jobrunr.jobs.details;

import org.jobrunr.utils.GraalVMUtils;
import org.jobrunr.utils.VersionNumber;

import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;
import static org.jobrunr.utils.reflection.ReflectionUtils.loadClass;

public class JobDetailsGeneratorFactory {

    private static final String JOB_DETAILS_CLASS_FILE_API_GENERATOR = "org.jobrunr.jobs.details.JobDetailsClassFileApiGenerator";

    private JobDetailsGeneratorFactory() {
    }

    public static JobDetailsGenerator createDefault() {
        return isJobDetailsClassFileApiGeneratorAvailable()
                ? jobDetailsClassFileApiGenerator()
                : jobDetailsAsmGenerator();
    }

    private static boolean isJobDetailsClassFileApiGeneratorAvailable() {
        return VersionNumber.JAVA_VERSION.hasMajorVersionHigherOrEqualTo(25)
                && !GraalVMUtils.isRunningInGraalVMNativeMode()
                && classExists(JOB_DETAILS_CLASS_FILE_API_GENERATOR);
    }

    private static JobDetailsGenerator jobDetailsClassFileApiGenerator() {
        try {
            return (JobDetailsGenerator) loadClass(JOB_DETAILS_CLASS_FILE_API_GENERATOR).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw shouldNotHappenException(e);
        }
    }

    private static JobDetailsGenerator jobDetailsAsmGenerator() {
        return new JobDetailsAsmGenerator();
    }
}
