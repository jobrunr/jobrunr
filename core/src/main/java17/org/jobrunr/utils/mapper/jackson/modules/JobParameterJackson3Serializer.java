package org.jobrunr.utils.mapper.jackson.modules;

import org.jobrunr.jobs.JobParameter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class JobParameterJackson3Serializer extends StdSerializer<JobParameter> {

    protected JobParameterJackson3Serializer() {
        super(JobParameter.class);
    }

    @Override
    public void serialize(JobParameter jobParameter, JsonGenerator jgen, SerializationContext provider) throws JacksonException {
        jgen.writeStartObject();
        jgen.writeStringProperty("className", jobParameter.getClassName());
        jgen.writeStringProperty("actualClassName", jobParameter.getActualClassName());
        jgen.writeStringProperty("object", jobParameter.getObject() != null ? jobParameter.getObject().toString() : null);
        jgen.writeEndObject();
    }
}
