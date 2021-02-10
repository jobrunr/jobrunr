package org.jobrunr.jobs.details;

import org.jobrunr.jobs.lambdas.IocJobLambda;

import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class JavaJobDetailsFinderContext extends JobDetailsFinderContext {

    public JavaJobDetailsFinderContext(SerializedLambda serializedLambda, Object... params) {
        super(initLocalVariables(serializedLambda, params), toFQClassName(serializedLambda.getImplClass()), serializedLambda.getImplMethodName());
    }


    protected static List<Object> initLocalVariables(SerializedLambda serializedLambda, Object[] params) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < serializedLambda.getCapturedArgCount(); i++) {
            final Object capturedArg = serializedLambda.getCapturedArg(i);
            result.add(capturedArg);
            //why: If the local variable at index is of type double or long, it occupies both index and index + 1. See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html
            if (capturedArg instanceof Long || capturedArg instanceof Double) {
                result.add(null);
            }
        }
        result.addAll(Arrays.asList(params));
        if (IocJobLambda.class.getName().equals(toFQClassName(serializedLambda.getFunctionalInterfaceClass()))) {
            result.add(null); // will be injected by IoC
        }
        return result;
    }

}
