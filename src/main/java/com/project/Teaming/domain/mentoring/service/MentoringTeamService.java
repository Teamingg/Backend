package com.project.Teaming.domain.mentoring.service;

import com.project.Teaming.domain.mentoring.dto.request.ParticipationRequest;
import com.project.Teaming.domain.mentoring.dto.request.TeamRequest;
import com.project.Teaming.domain.mentoring.dto.response.*;
import com.project.Teaming.domain.mentoring.entity.*;
import com.project.Teaming.domain.mentoring.provider.MentoringBoardDataProvider;
import com.project.Teaming.domain.mentoring.provider.MentoringParticipationDataProvider;
import com.project.Teaming.domain.mentoring.provider.MentoringTeamDataProvider;
import com.project.Teaming.domain.mentoring.provider.UserDataProvider;
import com.project.Teaming.domain.mentoring.repository.*;
import com.project.Teaming.domain.mentoring.service.policy.MentoringParticipationPolicy;
import com.project.Teaming.domain.mentoring.service.policy.MentoringTeamPolicy;
import com.project.Teaming.domain.user.entity.User;
import com.project.Teaming.domain.user.repository.UserRepository;
import com.project.Teaming.global.error.ErrorCode;
import com.project.Teaming.global.error.exception.BusinessException;
import com.project.Teaming.global.error.exception.MentoringTeamNotFoundException;
import com.project.Teaming.global.error.exception.NoAuthorityException;
import com.project.Teaming.global.jwt.dto.SecurityUserDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class MentoringTeamService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final MentoringTeamRepository mentoringTeamRepository;
    private final UserDataProvider userDataProvider;
    private final MentoringTeamDataProvider mentoringTeamDataProvider;
    private final MentoringTeamPolicy mentoringTeamPolicy;
    private final TeamCategoryService teamCategoryService;
    private final CategoryRepository categoryRepository;
    private final MentoringParticipationService mentoringParticipationService;
    private final MentoringParticipationRepository mentoringParticipationRepository;
    private final MentoringParticipationDataProvider mentoringParticipationDataProvider;
    private final MentoringParticipationPolicy mentoringParticipationPolicy;
    private final MentoringBoardRepository mentoringBoardRepository;
    private final RedisApplicantManagementService redisApplicantManagementService;

    /**
     * 멘토링팀 생성, 저장 로직
     * @param dto
     */

    @Transactional
    public Long saveMentoringTeam(TeamRequest dto) {

        MentoringTeam mentoringTeam = MentoringTeam.from(dto);

        MentoringTeam saved = mentoringTeamRepository.save(mentoringTeam);
        ParticipationRequest participationDto = new ParticipationRequest(MentoringAuthority.LEADER, MentoringParticipationStatus.ACCEPTED, dto.getRole());
        mentoringParticipationService.saveLeader(mentoringTeam.getId(), participationDto);
        //카테고리 생성
        teamCategoryService.saveTeamCategories(saved,dto.getCategories());
        return saved.getId();
    }

    /**
     * 멘토링 팀 수정 로직, 팀 구성원이며 팀장이여야 가능하다
     * @param mentoringTeamId
     * @param dto
     */
    @Transactional
    public void updateMentoringTeam(Long mentoringTeamId, TeamRequest dto) {
        User user = userDataProvider.getUser();

        MentoringTeam mentoringTeam = mentoringTeamDataProvider.findMentoringTeam(mentoringTeamId);

        mentoringParticipationPolicy.validateParticipation(
                mentoringTeam, user, MentoringAuthority.LEADER,MentoringParticipationStatus.ACCEPTED,
                () -> new BusinessException(ErrorCode.NOT_A_LEADER));

        mentoringTeamPolicy.validateTeamStatus(mentoringTeam);

        mentoringTeam.mentoringTeamUpdate(dto); //업데이트 메서드
        teamCategoryService.removeTeamCategories(mentoringTeam);
        teamCategoryService.saveTeamCategories(mentoringTeam, dto.getCategories());

    }


    /**
     * 특정 멘토링 팀을 찾는 로직
     * @param mentoringTeamId
     * @return
     */
    @Transactional(readOnly = true)
    public MentoringTeam findMentoringTeam(Long mentoringTeamId) {
        MentoringTeam mentoringTeam = mentoringTeamDataProvider.findMentoringTeam(mentoringTeamId);
        mentoringTeamPolicy.validateTeamStatus(mentoringTeam);
        return mentoringTeam;
    }

    @Transactional
    public MentoringTeam findMentoringTeamForUpdate(Long mentoringTeamId) {
        MentoringTeam mentoringTeam = mentoringTeamDataProvider.findMentoringTeam(mentoringTeamId);
        mentoringTeamPolicy.validateTeamStatus(mentoringTeam);
        return mentoringTeam;
    }

    /**
     * 내 멘토링 팀들을 모두 찾는 로직
     * @return
     */
    @Transactional(readOnly = true)
    public List<MentoringTeam> findMyMentoringTeams(Long userId) {
        User user = userDataProvider.findUser(userId);
        return mentoringTeamDataProvider.getTeamsByUserAndStatus(user,MentoringParticipationStatus.ACCEPTED);
    }

    @Transactional(readOnly = true)
    public List<MentoringTeam> getAuthenticateTeams() {
        return findMyMentoringTeams(userDataProvider.getUser().getId());
    }



    /**
     * 멘토링 팀 삭제 로직, 팀 구성원이고 리더여야 가능하다
     * @param mentoringTeamId
     */
    @Transactional
    public void deleteMentoringTeam(Long mentoringTeamId) {
        User user = userDataProvider.getUser();
        MentoringTeam mentoringTeam = mentoringTeamDataProvider.findMentoringTeam(mentoringTeamId);

        mentoringParticipationPolicy.validateParticipation(
                mentoringTeam, user, MentoringAuthority.LEADER,MentoringParticipationStatus.ACCEPTED,
                () -> new BusinessException(ErrorCode.NOT_A_LEADER));

        mentoringTeamPolicy.validateTeamStatus(mentoringTeam);
        mentoringTeam.flag(Status.TRUE);
        mentoringBoardRepository.deleteByTeamId(mentoringTeamId);
        // 영속성 컨텍스트 초기화
        entityManager.clear();

    }


    /**
     * 멘토링팀 responseDto 반환로직
     * 팀구성원이면 team정보만 반환,
     * 일반사용자용 페이지는 지원현황 포함해서 반환
     * @param team
     * @return
     */
    @Transactional(readOnly = true)
    public TeamAuthorityResponse getMentoringTeam(MentoringTeam team) {
        // 로그인 여부 확인 메서드
        User user = userDataProvider.getOptionalUser();
        TeamResponse dto = team.toDto();
        //리스폰스 dto생성
        TeamAuthorityResponse teamResponseDto = new TeamAuthorityResponse();
        teamResponseDto.setDto(dto);

        List<String> categories = categoryRepository.findCategoryIdsByTeamId(team.getId());
        dto.setCategories(categories);

        // 로그인하지 않은 사용자
        if (user == null) {
            handleNoAuthUser(teamResponseDto, team, null);
            return teamResponseDto;
        }

        //로그인 사용자
        mentoringParticipationRepository.findDynamicMentoringParticipation(team, user,null,MentoringParticipationStatus.ACCEPTED)
                .ifPresentOrElse(
                        participation -> {
                            // 권한 설정
                            teamResponseDto.setRole(participation.getAuthority());
                        },
                        () -> handleNoAuthUser(teamResponseDto, team, user) // Participation이 없는 경우 처리
                );

        return teamResponseDto;
    }

    /**
     * 나의 멘토링 팀 반환 DTO
     * @param team
     * @return
     */
    @Transactional(readOnly = true)
    public TeamInfoResponse getMyTeam(MentoringTeam team) {
        User user = userDataProvider.getUser();

        TeamInfoResponse teamDto = new TeamInfoResponse(team.getId(),
                team.getName(),
                team.getStartDate(),
                team.getEndDate(),
                team.getStatus());

        //권한 반환하는 로직
        MentoringParticipation teamUser = mentoringParticipationDataProvider.findParticipationWith(
                team, user,null, MentoringParticipationStatus.ACCEPTED,
                () -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM));

        teamDto.setRole(teamUser.getAuthority());
        teamDto.setCreatedDate(String.valueOf(team.getCreatedDate()));
        return teamDto;

    }

    @Transactional(readOnly = true)
    public TeamInfoResponse getTeamInfoWithAuthority(MentoringTeam team, User targetUser) {
        TeamInfoResponse teamDto = new TeamInfoResponse(
                team.getId(),
                team.getName(),
                team.getStartDate(),
                team.getEndDate(),
                team.getStatus()
        );

        // 특정 사용자의 권한을 조회
        MentoringParticipation teamUser = mentoringParticipationDataProvider.findParticipationWith(
                team, targetUser, null, MentoringParticipationStatus.ACCEPTED,
                () -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM)
        );

        teamDto.setRole(teamUser.getAuthority());
        teamDto.setCreatedDate(String.valueOf(team.getCreatedDate()));
        return teamDto;
    }

    private void handleNoAuthUser(TeamAuthorityResponse teamResponseDto, MentoringTeam team, User user) {
        teamResponseDto.setRole(MentoringAuthority.NoAuth);
        // 캐싱된 데이터 조회, dto 일반 사용자용으로 변환
        List<ParticipationForUserResponse> forUser = redisApplicantManagementService.getApplicants(team.getId()).stream()
                .map(ParticipationForUserResponse::forNoAuthUser)
                .toList();

        if (user != null) {
            setLoginStatus(forUser, user.getId());
        }
        teamResponseDto.setUserParticipations(forUser);
    }

    /**
     * MentoringStatus의 상태를 자동 변경
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    @Transactional
    public void updateMentoringStatus() {
        mentoringTeamRepository.updateStatusToWorking(MentoringStatus.WORKING, MentoringStatus.RECRUITING);
        mentoringTeamRepository.updateStatusToComplete(MentoringStatus.COMPLETE, MentoringStatus.WORKING);
        entityManager.clear();
    }

    /**
     * 로그인 한 사용자 있는지 확인하는 로직
     * @param dtos
     * @param userId
     */
    private void setLoginStatus(List<?> dtos, Long userId) {
        dtos.forEach(dto -> {
            if (dto instanceof TeamUserResponse teamDto && teamDto.getUserId().equals(userId)) {
                teamDto.setIsLogined(true);
            } else if (dto instanceof ParticipationForUserResponse userDto && userDto.getUserId().equals(userId)) {
                userDto.setIsLogined(true);
            }
        });
    }
}
