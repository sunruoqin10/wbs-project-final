package com.wbs.project.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
@Data
@AllArgsConstructor
public class PmHandoverResponse {
    private int handoveredCount;
    private List<String> projectIds;
}
