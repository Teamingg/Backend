package com.project.Teaming.domain.mentoring.repository;

import com.project.Teaming.domain.mentoring.dto.response.ParticipationForUserResponse;
import com.project.Teaming.domain.mentoring.dto.response.TeamParticipationResponse;
import com.project.Teaming.domain.mentoring.dto.response.TeamUserResponse;
import com.project.Teaming.domain.mentoring.entity.*;
import com.project.Teaming.domain.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepositoryCustom {

    List<TeamUserResponse> findAllByMemberStatus(
            MentoringTeam team,
            MentoringStatus teamStatus,
            MentoringParticipationStatus status,
            MentoringParticipationStatus status2,
            Long reviewerParticipationId);

    Optional<MentoringParticipation> findDynamicMentoringParticipation(
            MentoringTeam mentoringTeam,
            User user,
            MentoringAuthority authority,
            MentoringParticipationStatus status,
            List<MentoringParticipationStatus> statuses,
            Boolean isDeleted
    );

    List<TeamParticipationResponse> findAllForLeader(Long teamId, MentoringAuthority authority);

    List<ParticipationForUserResponse> findAllForUser(Long teamId, MentoringAuthority authority);
    Optional<MentoringParticipation> findFirstUser(Long teamId, MentoringParticipationStatus participationStatus, MentoringAuthority authority);
    long countBy(Long teamId, MentoringParticipationStatus status);


}