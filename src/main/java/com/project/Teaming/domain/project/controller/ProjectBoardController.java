package com.project.Teaming.domain.project.controller;

import com.project.Teaming.domain.project.dto.request.CreatePostDto;
import com.project.Teaming.domain.project.dto.response.ProjectPostInfoDto;
import com.project.Teaming.domain.project.dto.response.ProjectPostListDto;
import com.project.Teaming.domain.project.dto.response.ProjectPostStatusDto;
import com.project.Teaming.domain.project.service.ProjectBoardService;
import com.project.Teaming.domain.project.service.ProjectCacheService;
import com.project.Teaming.global.result.ResultCode;
import com.project.Teaming.global.result.pagenateResponse.PaginatedCursorResponse;
import com.project.Teaming.global.result.ResultListResponse;
import com.project.Teaming.global.result.ResultDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/project")
@Tag(name = "ProjectBoard", description = "프로젝트 팀의 게시글 관련 API")
public class ProjectBoardController {

    private final ProjectBoardService projectBoardService;
    private final ProjectCacheService projectCacheService;

    @PostMapping("/teams/{teamId}/posts")
    @Operation(summary = "프로젝트 글 등록", description = "프로젝트 팀에 대한 글 작성")
    public ResultDetailResponse<Void> createPost(@PathVariable Long teamId, @Valid @RequestBody CreatePostDto createPostDto) {
        projectBoardService.createPost(teamId, createPostDto);
        return new ResultDetailResponse<>(ResultCode.REGISTER_PROJECT_POST, null);
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "프로젝트 글 조회", description = "프로젝트 팀에서 작성한 게시물 상세 조회. isMember: 팀원 여부, isApply: 신청 여부")
    public ResultDetailResponse<ProjectPostInfoDto> getPostInfo(@PathVariable Long postId) {
        log.debug("success");
        ProjectPostInfoDto postInfoDto = projectBoardService.getPostInfo(postId);
        return new ResultDetailResponse<>(ResultCode.GET_PROJECT_POST_INFO, postInfoDto);
    }

    @PutMapping("/posts/{postId}")
    @Operation(summary = "프로젝트 글 수정", description = "팀원은 게시물 수정 가능. ")
    public ResultDetailResponse<Void> updatePost(@PathVariable Long postId, @Valid @RequestBody CreatePostDto createPostDto) {
        projectBoardService.updatePost(postId, createPostDto);
        return new ResultDetailResponse<>(ResultCode.UPDATE_PROJECT_POST_INFO, null);
    }

    @PatchMapping("/posts/{postId}")
    @Operation(summary = "게시물 모집 완료", description = "게시물을 팀원 또는 팀장이 모집 완료 처리를 직접 할 수 있다.")
    public ResultDetailResponse<ProjectPostStatusDto> completePostStatus(@PathVariable Long postId) {
        ProjectPostStatusDto dto = projectBoardService.completePostStatus(postId);
        return new ResultDetailResponse<>(ResultCode.GET_PROJECT_POST_STATUS, dto);
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "프로젝트 글 삭제", description = "팀에서 작성한 글 삭제.")
    public ResultDetailResponse<Void> deletePost(@PathVariable Long postId) {
        projectBoardService.deletePost(postId);
        return new ResultDetailResponse<>(ResultCode.DELETE_PROJECT_POST_INFO, null);
    }

    @GetMapping("/posts")
    @Operation(
            summary = "게시글 목록 조회",
            description = "커서 기반 페이징을 사용하여 게시글 목록을 조회한다. cursor를 기준으로 이후 게시글을 가져옵니다.",
            parameters = {
                    @Parameter(
                            name = "cursor",
                            description = "다음 게시글의 ID(다음 페이지 조회 시 필요. nextCursor 값)",
                            required = false
                    ),
                    @Parameter(
                            name = "pageSize",
                            description = "페이지당 게시글 수(기본값: 10)",
                            required = false,
                            schema = @Schema(type = "integer", example = "10", defaultValue = "10")
                    )
            }
    )
    public ResultDetailResponse<PaginatedCursorResponse<ProjectPostListDto>> getPosts(
            @RequestParam(required = false) Long cursor, // 마지막 게시글 ID
            @RequestParam(defaultValue = "10") int pageSize) {

        // 캐시에서 먼저 조회
        PaginatedCursorResponse<ProjectPostListDto> cachePosts = projectCacheService.getCachePosts(cursor, pageSize);

        if (cachePosts != null) {
            return new ResultDetailResponse<>(ResultCode.GET_PROJECT_POST_LIST, cachePosts);
        } else {
            // 캐시가 없으면 DB에서 조회하여 캐시에 저장
            PaginatedCursorResponse<ProjectPostListDto> posts = projectBoardService.getProjectPosts(cursor, pageSize);
            projectCacheService.cachePosts(cursor, pageSize, posts);
            return new ResultDetailResponse<>(ResultCode.GET_PROJECT_POST_LIST, posts);
        }
    }

    @GetMapping("/teams/{teamId}/posts")
    @Operation(summary = "특정 프로젝트 팀의 모든 글 조회", description = "특정 프로젝트 팀에서 작성한 글들을 조회")
    public ResultListResponse<ProjectPostListDto> getTeamPosts(@PathVariable Long teamId) {
        List<ProjectPostListDto> posts = projectBoardService.getTeamProjectPosts(teamId);
        return new ResultListResponse<>(ResultCode.GET_PROJECT_POST_LIST, posts);
    }
}
