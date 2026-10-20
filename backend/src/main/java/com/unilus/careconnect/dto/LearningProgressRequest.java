package com.unilus.careconnect.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class LearningProgressRequest {

    @NotNull
    @Min(1)
    @Max(20)
    private Integer section;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer score;

    public Integer getSection() { return section; }
    public void setSection(Integer section) { this.section = section; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
