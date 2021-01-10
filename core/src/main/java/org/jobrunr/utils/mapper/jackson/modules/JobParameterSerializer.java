package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.jobrunr.jobs.JobParameter;

import java.io.IOException;

public class JobParameterSerializer extends StdSerializer<JobParameter> {

    protected JobParameterSerializer() {
        super(JobParameter.class);
    }

    @Override
    public void serialize(JobParameter jobParameter, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("className", jobParameter.getClassName());
        jgen.writeStringField("actualClassName", jobParameter.getActualClassName());
        jgen.writeObjectField("object", jobParameter.getObject());
        jgen.writeEndObject();
    }

}
