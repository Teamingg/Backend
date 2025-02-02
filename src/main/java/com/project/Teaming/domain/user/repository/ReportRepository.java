package com.project.Teaming.domain.user.repository;

import com.project.Teaming.domain.mentoring.entity.MentoringParticipation;
import com.project.Teaming.domain.project.entity.ProjectParticipation;
import com.project.Teaming.domain.user.entity.Report;
import com.project.Teaming.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByProjectParticipationAndReportedUser(ProjectParticipation reporter, User reportedUser);
    boolean existsByMentoringParticipationAndReportedUser(MentoringParticipation reporter, User reportedUser);

    boolean existsByProjectParticipationAndReportedUserId(ProjectParticipation projectParticipation, Long reportedUserId);

    // 처리되지 않은 신고 수 조회
    long countByReportedUserIdAndProjectParticipation_ProjectTeamIdAndWarningProcessedFalse(Long reportedUserId, Long teamId);

    // warningProcessed를 true로 업데이트
    @Modifying
    @Query("UPDATE Report r SET r.warningProcessed = true WHERE r.reportedUser.id = :reportedUserId AND r.projectParticipation.projectTeam.id = :teamId AND r.warningProcessed = false")
    void updateWarningProcessedByReportedUserAndTeamId(@Param("reportedUserId") Long reportedUserId, @Param("teamId") Long teamId);

    @Query("SELECT r.reportedUser.id " +
            "FROM Report r " +
            "WHERE r.mentoringParticipation.id = :currentParticipationId " +
            "AND r.reportedUser.id IN :userIds")
    Set<Long> findReportedUserIds(@Param("currentParticipationId") Long currentParticipationId,
                                  @Param("userIds") Set<Long> userIds);
}
