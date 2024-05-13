package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jobrunr.jobs.JobParameter;

import java.util.BitSet;

public class JobRunrModule extends SimpleModule {

    public JobRunrModule() {
        addSerializer(JobParameter.class, new JobParameterSerializer());
        addSerializer(BitSet.class, new BitSetSerializer());
        addDeserializer(JobParameter.class, new JobParameterDeserializer());
        addDeserializer(BitSet.class, new BitSetDeserializer());
    }
}
