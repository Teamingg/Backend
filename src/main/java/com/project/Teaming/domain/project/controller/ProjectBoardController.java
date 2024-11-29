package com.project.Teaming.domain.project.controller;

import com.project.Teaming.domain.project.dto.request.CreatePostDto;
import com.project.Teaming.domain.project.dto.response.ProjectPostInfoDto;
import com.project.Teaming.domain.project.service.ProjectBoardService;
import com.project.Teaming.global.result.ResultCode;
import com.project.Teaming.global.result.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/project")
@Tag(name = "ProjectBoard", description = "프로젝트 팀의 게시글 관련 API")
public class ProjectBoardController {

    private final ProjectBoardService projectBoardService;

    @PostMapping("/post/{team_id}")
    @Operation(summary = "프로젝트 팀 게시물 작성", description = "프로젝트 팀에 대한 모집 게시글 작성")
    public ResultResponse<Void> createPost(@PathVariable Long team_id, @Valid @RequestBody CreatePostDto createPostDto) {
        projectBoardService.createPost(team_id, createPostDto);
        return new ResultResponse<>(ResultCode.REGISTER_PROJECT_POST, null);
    }

    @GetMapping("/post/{team_id}/{post_id}")
    @Operation(summary = "프로젝트 팀 게시물 상세 조회", description = "프로젝트 팀에서 작성한 게시물들 중 하나에 대한 상세 조회")
    public ResultResponse<ProjectPostInfoDto> getPostInfo(@PathVariable Long team_id, @PathVariable Long post_id) {
        ProjectPostInfoDto postInfoDto = projectBoardService.getPostInfo(team_id, post_id);
        return new ResultResponse<>(ResultCode.GET_PROJECT_POST_INFO, List.of(postInfoDto));
    }

    @PutMapping("/post/{team_id}/{post_id}/edit")
    @Operation(summary = "프로젝트 팀 게시물 수정", description = "게시물 수정")
    public ResultResponse<Void> updatePost(@PathVariable Long team_id, @PathVariable Long post_id, @Valid @RequestBody CreatePostDto createPostDto) {
        projectBoardService.updatePost(team_id, post_id, createPostDto);
        return new ResultResponse<>(ResultCode.UPDATE_PROJECT_POST_INFO, null);
    }

    @DeleteMapping("/post/{team_id}/{post_id}")
    @Operation(summary = "프로젝트 팀 게시물 삭제", description = "프로젝트 팀에서 작성한 모집 게시글 삭제.")
    public ResultResponse<Void> deletePost(@PathVariable Long team_id, @PathVariable Long post_id) {
        projectBoardService.deletePost(team_id, post_id);
        return new ResultResponse<>(ResultCode.DELETE_PROJECT_POST_INFO, null);
    }
}
