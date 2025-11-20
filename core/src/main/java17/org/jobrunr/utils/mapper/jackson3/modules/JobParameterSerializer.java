package org.jobrunr.utils.mapper.jackson3.modules;

import org.jobrunr.jobs.JobParameter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class JobParameterSerializer extends StdSerializer<JobParameter> {

    protected JobParameterSerializer() {
        super(JobParameter.class);
    }

    @Override
    public void serialize(JobParameter jobParameter, JsonGenerator jgen, SerializationContext provider) throws JacksonException {
        jgen.writeStartObject();
        jgen.writeStringProperty("className", jobParameter.getClassName());
        jgen.writeStringProperty("actualClassName", jobParameter.getActualClassName());
        Object objectValue = jobParameter.getObject();
        if (objectValue != null) {
            jgen.writeName("object");
            jgen.writePOJO(objectValue);
        } else {
            jgen.writeNullProperty("object");
        }
        jgen.writeEndObject();
    }
}
