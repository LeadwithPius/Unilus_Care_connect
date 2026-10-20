package com.unilus.careconnect.dto;

import java.util.List;

public record LearningProgressResponse(
        List<Integer> completedSections,
        int completedCount,
        int totalSections,
        int progressPercent) {
}
