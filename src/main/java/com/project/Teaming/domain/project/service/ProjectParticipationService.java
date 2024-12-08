package com.project.Teaming.domain.project.service;

import com.project.Teaming.domain.project.dto.request.JoinTeamDto;
import com.project.Teaming.domain.project.dto.response.ProjectParticipationInfoDto;
import com.project.Teaming.domain.project.entity.ParticipationStatus;
import com.project.Teaming.domain.project.entity.ProjectParticipation;
import com.project.Teaming.domain.project.entity.ProjectRole;
import com.project.Teaming.domain.project.entity.ProjectTeam;
import com.project.Teaming.domain.project.repository.ProjectParticipationRepository;
import com.project.Teaming.domain.project.repository.ProjectTeamRepository;
import com.project.Teaming.domain.user.entity.User;
import com.project.Teaming.domain.user.repository.UserRepository;
import com.project.Teaming.global.error.ErrorCode;
import com.project.Teaming.global.error.exception.BusinessException;
import com.project.Teaming.global.jwt.dto.SecurityUserDto;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectParticipationService {

    private final ProjectParticipationRepository projectParticipationRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final UserRepository userRepository;

    public void createParticipation(ProjectTeam projectTeam) {
        ProjectParticipation projectParticipation = new ProjectParticipation();
        User user = userRepository.findById(getCurrentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_USER));

        projectParticipation.createProjectParticipation(user, projectTeam);
        projectParticipationRepository.save(projectParticipation);
    }

    private Long getCurrentId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserDto securityUser = (SecurityUserDto) authentication.getPrincipal();
        return securityUser.getUserId();
    }

    public void joinTeam(JoinTeamDto dto) {
        ProjectTeam projectTeam = projectTeamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_TEAM));

        User user = userRepository.findById(getCurrentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_USER));

        // 이미 팀에 참여했는지 여부 확인
        Optional<ProjectParticipation> existingParticipation = projectParticipationRepository.findByProjectTeamIdAndUserId(dto.getTeamId(), user.getId());
        if (existingParticipation.isPresent()) {
            ProjectParticipation participation = existingParticipation.get();
            if (participation.getRole() == ProjectRole.OWNER) {
                throw new BusinessException(ErrorCode.ALREADY_PARTICIPATED_OWNER);
            } else if (participation.getRole() == ProjectRole.MEMBER) {
                throw new BusinessException(ErrorCode.ALREADY_PARTICIPATED_MEMBER);
            }
        }

        // 새로운 팀에 참여
        ProjectParticipation newParticipation = new ProjectParticipation();
        newParticipation.joinTeamMember(user, projectTeam, dto.getRecruitCategory());
        projectParticipationRepository.save(newParticipation);
    }

    public void cancelTeam(Long teamId) {
        User user = userRepository.findById(getCurrentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_USER));

        ProjectParticipation participation = projectParticipationRepository.findByProjectTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_PARTICIPATION));

        if (participation.getParticipationStatus() == ParticipationStatus.PENDING && !participation.getIsDeleted()) {
            projectParticipationRepository.delete(participation);
        } else {
            throw new BusinessException(ErrorCode.INVALID_PARTICIPATION_ERROR);
        }
    }

    public void quitTeam(Long teamId) {
        User user = userRepository.findById(getCurrentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_USER));

        ProjectParticipation participation = projectParticipationRepository.findByProjectTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_PARTICIPATION));

        if (participation.canQuit()) {
            participation.quitTeam();
        } else {
            throw new BusinessException(ErrorCode.CANNOT_QUIT_TEAM);
        }
    }

    public void acceptedMember(Long teamId, Long userId) {
        User user = userRepository.findById(getCurrentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_USER));
        ProjectParticipation joinMember = projectParticipationRepository.findByProjectTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_PARTICIPATION));
        ProjectParticipation teamOwner = projectParticipationRepository.findByProjectTeamIdAndRole(teamId, ProjectRole.OWNER)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_OWNER));

        if (joinMember.canAccept() && isTeamOwner(user, teamOwner)) {
            joinMember.acceptTeam();
        } else {
            throw new BusinessException(ErrorCode.CANNOT_ACCEPT_MEMBER);
        }
    }

    public void rejectedMember(Long teamId, Long userId) {
        User user = userRepository.findById(getCurrentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_USER));
        ProjectParticipation joinMember = projectParticipationRepository.findByProjectTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_PARTICIPATION));
        ProjectParticipation teamOwner = projectParticipationRepository.findByProjectTeamIdAndRole(teamId, ProjectRole.OWNER)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_OWNER));

        if (joinMember.canReject() && isTeamOwner(user, teamOwner)) {
            joinMember.rejectTeam();
        } else {
            throw new BusinessException(ErrorCode.CANNOT_REJECT_MEMBER);
        }
    }

    public List<ProjectParticipationInfoDto> getAllParticipationDtos(Long teamId) {
        return projectParticipationRepository.findByProjectTeamId(teamId).stream()
                .map(ProjectParticipationInfoDto::new)
                .collect(Collectors.toList());
    }

    public void exportMember(Long teamId, Long userId) {
        User user = userRepository.findById(getCurrentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_USER));
        ProjectParticipation exportMember = projectParticipationRepository.findByProjectTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_PARTICIPATION));
        ProjectParticipation teamOwner = projectParticipationRepository.findByProjectTeamIdAndRole(teamId, ProjectRole.OWNER)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_OWNER));

        if (isTeamOwner(user, teamOwner)) {
            exportMember.exportTeam();
        } else {
            throw new BusinessException(ErrorCode.FAIL_TO_EXPORT_TEAM);
        }
    }

    private static boolean isTeamOwner(User user, ProjectParticipation teamOwner) {
        return user.getId().equals(teamOwner.getUser().getId());
    }
}
