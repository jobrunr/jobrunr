package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.ANY)
public abstract class SavingsMixin {
    @JsonCreator
    public SavingsMixin(
            @JsonProperty("chronoUnit") ChronoUnit chronoUnit,
            @JsonProperty("period") Temporal period,
            @JsonProperty("totalSavings") BigDecimal totalSavings
    ) {}
}
