package com.wbs.project.dto;
import lombok.Data;
import java.util.List;
@Data
public class HandoverPreviewResponse {
    private OutgoingBrief outgoing;
    private List<ProjectBrief> candidateProjects;
    private List<String> candidateDeptCodes;

    @Data
    public static class OutgoingBrief {
        private String userId;
        private String name;
        private String role;
        private String deptCode;
    }

    @Data
    public static class ProjectBrief {
        private String id;
        private String name;
        private String status;
        private String deptCode;
        private String previousOwnerId;
        private Boolean needsHandover;
    }
}
