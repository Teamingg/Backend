package com.project.Teaming.domain.project.controller;

import com.project.Teaming.domain.project.dto.request.CreateTeamDto;
import com.project.Teaming.domain.project.dto.request.UpdateTeamDto;
import com.project.Teaming.domain.project.dto.response.ProjectTeamInfoDto;
import com.project.Teaming.domain.project.entity.ProjectTeam;
import com.project.Teaming.domain.project.service.ProjectParticipationService;
import com.project.Teaming.domain.project.service.ProjectTeamService;
import com.project.Teaming.global.result.ResultCode;
import com.project.Teaming.global.result.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@Tag(name = "ProjectTeam", description = "프로젝트 팀 관련 API")
public class ProjectTeamController {

    private final ProjectTeamService projectTeamService;
    private final ProjectParticipationService projectParticipationService;

    @PostMapping("/project/team")
    @Operation(summary = "프로젝트 팀 생성", description = "프로젝트를 생성하고 싶은 사용자는 프로젝트 팀 생성을 통해 프로젝트를 생성할 수 있고 해당 프로젝트의 팀장이 된다.")
    public ResultResponse<Void> createTeam(@RequestBody CreateTeamDto createTeamDto, BindingResult bindingResult) {
        ProjectTeam projectTeam = projectTeamService.createTeam(createTeamDto);
        projectParticipationService.createParticipation(projectTeam);
        return new ResultResponse<>(ResultCode.REGISTER_PROJECT_TEAM, null);
    }

    @GetMapping("/project/team/{team_id}")
    @Operation(summary = "프로젝트 팀 정보 조회", description = "프로젝트 팀의 정보를 조회한다.")
    public ResultResponse<ProjectTeamInfoDto> getTeam(@PathVariable Long team_id) {
        ProjectTeamInfoDto dto = projectTeamService.getTeam(team_id);
        return new ResultResponse<>(ResultCode.GET_PROJECT_TEAM, List.of(dto));
    }

    @PutMapping("/project/team/{team_id}/edit")
    @Operation(summary = "프로젝트 팀 정보 수정", description = "프로젝트 팀의 정보를 수정한다.")
    public ResultResponse<Void> editTeam(@PathVariable Long team_id, @RequestBody UpdateTeamDto updateTeamDto) {
        projectTeamService.editTeam(team_id, updateTeamDto);
        return new ResultResponse<>(ResultCode.UPDATE_PROJECT_TEAM, null);
    }

    @DeleteMapping("project/team/{team_id}/delete")
    @Operation(summary = "프로젝트 팀 삭제", description = "프로젝트 팀을 삭제한다.")
    public ResultResponse<Void> deleteTeam(@PathVariable Long team_id) {
        projectTeamService.deleteTeam(team_id);
        return new ResultResponse<>(ResultCode.DELETE_PROJECT_TEAM, null);
    }
}
