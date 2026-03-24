package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.annotation.JsonProperty;


public abstract class JobStateMixin {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String state;
}
