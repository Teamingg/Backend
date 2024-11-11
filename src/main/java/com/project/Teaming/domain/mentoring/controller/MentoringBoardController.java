package com.project.Teaming.domain.mentoring.controller;


import com.project.Teaming.domain.mentoring.dto.request.RqBoardDto;
import com.project.Teaming.domain.mentoring.dto.response.BoardResponseDto;
import com.project.Teaming.domain.mentoring.dto.response.RsBoardDto;
import com.project.Teaming.domain.mentoring.entity.MentoringBoard;
import com.project.Teaming.domain.mentoring.service.MentoringBoardService;
import com.project.Teaming.domain.user.entity.User;
import com.project.Teaming.domain.user.service.UserService;
import com.project.Teaming.global.error.exception.NoAuthorityException;
import com.project.Teaming.global.jwt.dto.SecurityUserDto;
import com.project.Teaming.global.result.ResultCode;
import com.project.Teaming.global.result.ResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/mentoring")
@Tag(name = "MentoringBoard", description = "멘토링 글 관련 API")
public class MentoringBoardController {

    private final MentoringBoardService mentoringBoardService;
    private final UserService userService;

    @PostMapping("/{team_id}/post")
    @Operation(summary = "멘토링 글 등록" , description = "멘토링 팀에서 글을 등록 할 수 있다. 멘토링 게시판 페이지로 이동")
    public ResultResponse<Void> savePost(@PathVariable Long team_id,
                                         @RequestBody @Valid  RqBoardDto dto,
                                         BindingResult bindingResult) {
        mentoringBoardService.saveMentoringPost(team_id, dto);
        return new ResultResponse<>(ResultCode.REGISTER_MENTORING_POST, null);
    }


    @PostMapping("/{team_id}/post/{post_id}")
    @Operation(summary = "멘토링 글 수정", description = "나의 팀에서 등록된 멘토링 게시물을 수정 할 수 있다. 업데이트 된 멘토링 포스트로 이동")
    public ResultResponse<RsBoardDto> updatePost(@PathVariable Long team_id, @PathVariable Long post_id,
                                                 @RequestBody @Valid RqBoardDto dto,
                                                 BindingResult bindingResult) {
        User user = getUser();
        List<Long> team = getTeam(team_id, user);
        if (!ObjectUtils.isEmpty(team)) {
            mentoringBoardService.updateMentoringPost(post_id,dto);
            MentoringBoard mentoringPost = mentoringBoardService.findMentoringPost(post_id);
            RsBoardDto updatePostDto = mentoringPost.toDto();
            return new ResultResponse<>(ResultCode.UPDATE_MENTORING_POST, List.of(updatePostDto));
        } else throw new NoAuthorityException("글을 수정할 수 있는 권한이 없습니다.");
    }



    @GetMapping("/posts")
    @Operation(summary = "멘토링 글 모두 조희" , description = "모든 멘토링 게시물들을 조희할 수 있다. 멘토링 게시판 보여 줄 때의 API")
    public ResultResponse<RsBoardDto> findAllPosts() {
        List<RsBoardDto> boards = mentoringBoardService.findAllMentoringPost().stream()
                .map(o -> RsBoardDto.builder()  //id, 제목, 모집하는 역할, 멘토링 팀 상태 반환.
                        .id(o.getId())
                        .title(o.getTitle())
                        .role(o.getRole())
                        .startDate(o.getStartDate())
                        .endDate(o.getEndDate())
                        .status(o.getStatus())
                        .build())
                .collect(Collectors.toList());
        return new ResultResponse<>(ResultCode.GET_ALL_MENTORING_POSTS, boards);
    }

    @GetMapping("/{team_Id}/posts")
    @Operation(summary = "특정 멘토링 팀의 모든 글 조회" , description = "특정 멘토링 팀에서 쓴 모든 글을 조회 할 수 있다. 팀 페이지에서 시용")
    public ResultResponse<RsBoardDto> findMyAllPosts(@PathVariable Long team_Id) {
        List<RsBoardDto> myBoards = mentoringBoardService.findAllMyMentoringPost(team_Id)
                .stream().map(
                        o -> RsBoardDto.builder()
                                .id(o.getId())
                                .title(o.getTitle())
                                .role(o.getRole())
                                .startDate(o.getStartDate())
                                .endDate(o.getEndDate())
                                .status(o.getStatus())
                                .build()
                )
                .collect(Collectors.toList());
        return new ResultResponse<>(ResultCode.GET_ALL_MY_MENTORING_POSTS, myBoards);
    }

    /**
     * 특정 글 조회하는 API
     * 현재 로그인한 유저가 가지고있는 멘토링 팀 ID들을 모두 프론트에게 같이 보내줌
     * 프론트가 ID비교해 내 팀의 글이면 수정하기, 삭제하기 버튼 보여줌.
     * @param post_id
     * @return
     */
    @GetMapping("/post/{post_id}")
    @Operation(summary = "멘토링 글 조희" , description = "멘토링 게시판에서 특정 멘토링 글을 조회할 수 있다. " +
            "현재 로그인 된 유저의 멘토링 팀 ID들과 조회된 post의 team의 ID를 같이 반환, 프론트에서 비교 필요(동일한 ID 존재하면 수정하기, 삭제하기 버튼, 존재하지 않으면 그냥 조회).")
    public ResultResponse<BoardResponseDto> findPost(@PathVariable Long post_id) {
        User user = getUser();
        BoardResponseDto responseDto = new BoardResponseDto();
        List<Long> teams = user.getMentoringParticipations().stream()
                .map(o -> o.getMentoringTeam().getId())
                .toList();
        MentoringBoard mentoringPost = mentoringBoardService.findMentoringPost(post_id);
        RsBoardDto dto = mentoringPost.toDto();  //현재 포스트를 쓴 MentoringTeam의 id가 들어가있다
        responseDto.setTeamId(teams);  //프론트에서 비교해 줄 현재 로그인 된 유저의 멘토링 팀 ID들
        responseDto.setDto(dto); //조회된 post의 dto(teamId 포함)
        return new ResultResponse<>(ResultCode.GET_MENTORING_POST, List.of(responseDto));
    }

    @PostMapping("/{team_id}/post/{post_id}/del")
    @Operation(summary = "멘토링 글 삭제", description = "나의 멘토링 글을 삭제 할 수 있다. 멘토링 게시판으로 이동")
    public ResultResponse<Void> deletePost(@PathVariable Long team_id, @PathVariable Long post_id) {
        User user = getUser();
        List<Long> team = getTeam(team_id, user);
        if (!ObjectUtils.isEmpty(team)) {
            mentoringBoardService.deleteMentoringPost(post_id);
        } else throw new NoAuthorityException("글을 삭제할 수 있는 권한이 없습니다.");
        return new ResultResponse<>(ResultCode.DELETE_MENTORING_POST, null);
    }


    private List<Long> getTeam(Long team_id, User user) {
        return user.getMentoringParticipations().stream()
                .map(o -> o.getMentoringTeam().getId())
                .filter(o -> o.equals(team_id))
                .toList();
    }

    private User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserDto securityUser = (SecurityUserDto) authentication.getPrincipal();
        Long userId = securityUser.getUserId();
        User user = userService.findById(userId).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return user;
    }

}
