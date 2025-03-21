package com.project.Teaming.domain.project.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.Teaming.domain.project.entity.PostStatus;
import com.project.Teaming.domain.project.entity.ProjectBoard;
import com.project.Teaming.domain.project.entity.ProjectTeam;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectPostListDto {

    private String title;
    private String teamName;

    private String startDate;
    private String endDate;
    private String contents;
    private PostStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long projectTeamId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long postId;
    private String createdDate;

    private List<String> stacks;  // 기술 스택(id 조회)

    public static ProjectPostListDto from(ProjectTeam projectTeam, ProjectBoard projectBoard, List<String> stackIds) {
        ProjectPostListDto dto = new ProjectPostListDto();
        dto.setTitle(projectBoard.getTitle());
        dto.setTeamName(projectTeam.getName());
        dto.setStartDate(String.valueOf(projectTeam.getStartDate()));
        dto.setEndDate(String.valueOf(projectTeam.getEndDate()));
        dto.setContents(projectBoard.getContents());
        dto.setStatus(projectBoard.getStatus());
        dto.setProjectTeamId(projectTeam.getId());
        dto.setPostId(projectBoard.getId());
        dto.setCreatedDate(dto.getFormattedDate(projectBoard.getCreatedDate()));
        dto.setStacks(stackIds);
        return dto;
    }

    public String getFormattedDate(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }
}
