package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.jobrunr.jobs.JobParameter;

import java.io.IOException;

public class JobRunrModule extends SimpleModule {

    public JobRunrModule() {
        addSerializer(JobParameter.class, new JobParameterSerializer());
        addDeserializer(JobParameter.class, new JobParameterDeserializer());
    }
}
