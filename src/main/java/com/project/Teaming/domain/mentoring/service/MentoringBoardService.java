package com.project.Teaming.domain.mentoring.service;

import com.project.Teaming.domain.mentoring.dto.request.RqBoardDto;
import com.project.Teaming.domain.mentoring.dto.response.MentoringPostStatusDto;
import com.project.Teaming.domain.mentoring.dto.response.RsBoardDto;
import com.project.Teaming.domain.mentoring.dto.response.RsSpecBoardDto;
import com.project.Teaming.domain.mentoring.entity.*;
import com.project.Teaming.domain.mentoring.repository.MentoringBoardRepository;
import com.project.Teaming.domain.mentoring.repository.MentoringParticipationRepository;
import com.project.Teaming.domain.mentoring.repository.MentoringTeamRepository;
import com.project.Teaming.domain.user.entity.User;
import com.project.Teaming.domain.user.repository.UserRepository;
import com.project.Teaming.global.error.ErrorCode;
import com.project.Teaming.global.error.exception.BusinessException;
import com.project.Teaming.global.error.exception.MentoringPostNotFoundException;
import com.project.Teaming.global.error.exception.MentoringTeamNotFoundException;
import com.project.Teaming.global.error.exception.NoAuthorityException;
import com.project.Teaming.global.jwt.dto.SecurityUserDto;
import com.project.Teaming.global.result.pagenateResponse.PaginatedCursorResponse;
import com.project.Teaming.global.result.pagenateResponse.PaginatedResponse;
import com.querydsl.core.Tuple;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentoringBoardService {

    @PersistenceContext
    EntityManager entityManager;

    private final MentoringBoardRepository mentoringBoardRepository;
    private final MentoringTeamRepository mentoringTeamRepository;
    private final UserRepository userRepository;
    private final MentoringParticipationRepository mentoringParticipationRepository;

    /**
     * 게시물을 저장하는 로직
     * 저장된 post Id 반환
     * @param teamId
     * @param boardDto
     */
    @Transactional
    public Long saveMentoringPost(Long teamId, RqBoardDto boardDto) {
        User user = getUser();
        MentoringTeam mentoringTeam = mentoringTeamRepository.findById(teamId).orElseThrow(MentoringTeamNotFoundException::new);  //명시적 조회, 최신 데이터 반영
        Optional<MentoringParticipation> TeamUser = mentoringParticipationRepository.findByMentoringTeamAndUserAndParticipationStatus(mentoringTeam, user, MentoringParticipationStatus.ACCEPTED);
        if (TeamUser.isPresent() && !TeamUser.get().getIsDeleted()) {
            MentoringBoard mentoringBoard = MentoringBoard.builder()
                    .title(boardDto.getTitle())
                    .contents(boardDto.getContents())
                    .role(boardDto.getRole())
                    .status(PostStatus.RECRUITING)
                    .deadLine(boardDto.getDeadLine())
                    .mentoringCnt(boardDto.getMentoringCnt())
                    .build();
            mentoringBoard.setLink(Optional.ofNullable(boardDto.getLink())
                    .orElse(mentoringTeam.getLink()));
            mentoringBoard.addMentoringBoard(mentoringTeam);  // 멘토링 팀과 연관관계 매핑

            MentoringBoard savedPost = mentoringBoardRepository.save(mentoringBoard);
            return savedPost.getId();
        } else throw new NoAuthorityException(ErrorCode.NO_AUTHORITY);
    }

    /**
     * 특정 게시물을 조회하는 로직
     * @param postId
     * @return
     */
    @Transactional(readOnly = true)
    public MentoringBoard findMentoringPost(Long postId) {
        MentoringBoard mentoringBoard = mentoringBoardRepository.findById(postId)
                .orElseThrow(() -> new MentoringPostNotFoundException("이미 삭제되었거나 존재하지 않는 글 입니다."));
        if (mentoringBoard.getMentoringTeam().getFlag() == Status.FALSE) { //team의 최신 데이터 업데이트
            return mentoringBoard;
        } else {
            throw new MentoringTeamNotFoundException("이미 삭제된 팀의 글 입니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<String> findTeamCategories(Long teamId) {
        List<Object[]> teamCategories = mentoringBoardRepository.findAllCategoriesByMentoringTeamId(teamId);
        return teamCategories.stream()
                .map(x -> String.valueOf(x[1]))
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RsSpecBoardDto toDto(MentoringBoard mentoringPost) {
        User user = getUser();
        MentoringTeam mentoringTeam = mentoringPost.getMentoringTeam();
        RsSpecBoardDto dto = mentoringPost.toDto(mentoringTeam);
        List<String> teamCategories = findTeamCategories(mentoringTeam.getId());
        dto.setCategory(teamCategories);
        Optional<MentoringParticipation> teamUser = mentoringParticipationRepository.findByMentoringTeamAndUserAndParticipationStatus(mentoringPost.getMentoringTeam(), user,MentoringParticipationStatus.ACCEPTED);
        if (teamUser.isPresent() && !teamUser.get().getIsDeleted()) {
            dto.setAuthority(teamUser.get().getAuthority());
        } else {
            dto.setAuthority(MentoringAuthority.NoAuth);
        }
        return dto;
    }

    /**
     * 특정 멘토링 팀의 삭제되지않은 모든 게시물들을 가져오는 로직
     * @param teamId
     * @return
     */
    @Transactional(readOnly = true)
    public List<RsBoardDto> findAllMyMentoringPost(Long teamId) {
        List<RsBoardDto> boards = mentoringBoardRepository.findAllByMentoringTeamId(teamId);
        List<Object[]> categoryResults = mentoringBoardRepository.findAllCategoriesByMentoringTeamId(teamId);
        MentoringTeam mentoringTeam = mentoringTeamRepository.findById(teamId).orElseThrow(MentoringTeamNotFoundException::new);
        if (mentoringTeam.getFlag() == Status.FALSE) {
            Map<Long, List<String>> categoryMap = new HashMap<>();
            for (Object[] row : categoryResults) {
                Long boardId = (Long) row[0];
                String categoryId = row[1] != null ? String.valueOf(row[1]) : null;
                categoryMap.computeIfAbsent(boardId, k -> new ArrayList<>()).add(categoryId);
            }
            boards.forEach(post -> {
                List<String> categories = categoryMap.getOrDefault(post.getBoardId(), Collections.emptyList());
                post.setCategory(categories);
            });
            return boards;
        }else throw new MentoringTeamNotFoundException("이미 삭제 된 멘토링 팀 입니다.");
    }

    /**
     * 삭제되지 않은 모든 게시물들을 가져오는 로직
     *
     * @return
*/
    @Transactional(readOnly = true)
    public PaginatedCursorResponse<RsBoardDto> findAllPosts(Long cursor, int size) {
        List<Long> ids = mentoringBoardRepository.findMentoringBoardIds(cursor, size + 1, Status.FALSE);

        // 2. 다음 페이지 여부 확인
        boolean isLast = ids.size() <= size;
        if (!isLast) {
            ids = ids.subList(0, size); // 추가된 데이터 제외하고 요청 크기만큼 자르기
        }

        List<MentoringBoard> boards = mentoringBoardRepository.findAllByIds(ids);
        List<MentoringBoard> distinctBoards = boards.stream()
                .distinct()
                .toList();
        List<RsBoardDto> boardDtos = distinctBoards.stream()
                .map(board -> {
                    MentoringTeam mentoringTeam = board.getMentoringTeam();
                    List<String> categories = mentoringTeam.getCategories().stream()
                            .map(category -> String.valueOf(category.getCategory().getId()))
                            .distinct()
                            .toList();
                    return RsBoardDto.from(board, mentoringTeam, categories);
                })
                .toList();

        //  다음 커서 설정
        Long nextCursor = (distinctBoards.isEmpty() || isLast) ? null : distinctBoards.get(distinctBoards.size() - 1).getId();


        return new PaginatedCursorResponse<>(
                boardDtos,
                nextCursor, // 다음 커서 값 반환
                size,
                isLast // 마지막 페이지 여부
        );

    }

    /**
     *  게시물을 수정하는 로직
     * @param postId
     * @param dto
     * @return
     */
    @Transactional
    public void updateMentoringPost(Long postId, RqBoardDto dto) {
        User user = getUser();
        MentoringBoard mentoringBoard = mentoringBoardRepository.findById(postId)
                .orElseThrow(() -> new MentoringPostNotFoundException("이미 삭제되었거나 존재하지 않는 글 입니다."));
        MentoringTeam mentoringTeam = mentoringBoard.getMentoringTeam();
        Optional<MentoringParticipation> TeamUser = mentoringParticipationRepository.findByMentoringTeamAndUserAndParticipationStatus(mentoringTeam, user, MentoringParticipationStatus.ACCEPTED);
        if (mentoringTeam.getFlag() == Status.FALSE) {  //team의 최신 데이터 업데이트
            if (TeamUser.isPresent() && !TeamUser.get().getIsDeleted()) {
                mentoringBoard.updateBoard(dto);
            } else throw new BusinessException(ErrorCode.NO_AUTHORITY);
        } else {
                throw new MentoringTeamNotFoundException("이미 삭제된 팀의 글 입니다.");
            }
    }

    /**
     * 게시물을 삭제하는 로직
     * @param postId
     */
    @Transactional
    public void deleteMentoringPost(Long postId) {
        User user = getUser();
        MentoringBoard mentoringBoard = mentoringBoardRepository.findById(postId)
                .orElseThrow(() -> new MentoringPostNotFoundException("이미 삭제되었거나 존재하지 않는 글 입니다."));
        Optional<MentoringParticipation> teamUser = mentoringParticipationRepository.findByMentoringTeamAndUserAndParticipationStatus(mentoringBoard.getMentoringTeam(), user, MentoringParticipationStatus.ACCEPTED);
        if (teamUser.isPresent() && !teamUser.get().getIsDeleted()) {
            if (mentoringBoard.getMentoringTeam().getFlag() == Status.FALSE) { //team의 최신 데이터 업데이트
                mentoringBoardRepository.delete(mentoringBoard);
            } else {
                throw new MentoringTeamNotFoundException("이미 삭제된 팀의 글 입니다.");
            }
        }
        else throw new NoAuthorityException(ErrorCode.NO_AUTHORITY);
    }

    /**
     * 팀원이 글에서 모집 현황을 수정할 수 있는 로직
     * @param teamId
     * @param postId
     * @return
     */
    @Transactional
    public MentoringPostStatusDto updatePostStatus(Long teamId, Long postId) {
        User user = getUser();
        MentoringTeam mentoringTeam = mentoringTeamRepository.findById(teamId).orElseThrow(MentoringTeamNotFoundException::new);
        Optional<MentoringParticipation> teamUser = mentoringParticipationRepository.findByMentoringTeamAndUserAndParticipationStatus(mentoringTeam, user, MentoringParticipationStatus.ACCEPTED);
        MentoringBoard post = mentoringBoardRepository.findById(postId).orElseThrow(MentoringPostNotFoundException::new);
        if (teamUser.isPresent() && !teamUser.get().getIsDeleted()) {
            post.updateStatus();
            return new MentoringPostStatusDto(PostStatus.COMPLETE);
        } else {
            throw new NoAuthorityException(ErrorCode.NOT_A_MEMBER);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정 실행
    @Transactional
    public void updateCheckCompleteStatus() {
        mentoringBoardRepository.bulkUpDateStatus(PostStatus.COMPLETE, PostStatus.RECRUITING, LocalDate.now());
        entityManager.clear();
    }

    private User getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserDto securityUser = (SecurityUserDto) authentication.getPrincipal();
        Long userId = securityUser.getUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        return user;
    }

}
