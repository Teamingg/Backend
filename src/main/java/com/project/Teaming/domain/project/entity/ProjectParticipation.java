package com.project.Teaming.domain.project.entity;

import com.project.Teaming.domain.user.entity.Report;
import com.project.Teaming.domain.user.entity.User;
import com.project.Teaming.global.error.ErrorCode;
import com.project.Teaming.global.error.exception.BusinessException;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "project_participation")
@NoArgsConstructor
@AllArgsConstructor
public class ProjectParticipation {
    @Id
    @Tsid
    @Column(name = "par_id")
    private Long id;  // 신청 ID

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ParticipationStatus participationStatus;  // 신청 상태

    @Column(name = "is_deleted")
    private Boolean isDeleted;  // 탈퇴 여부 (초기값 : false)

    @Column(name = "is_export")
    private Boolean isExport;  // 팀장의 내보기내 여부 (초기값: false)

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime requestDate;  // 신청일

    private LocalDateTime decisionDate;  // 신청 수락/거절 날짜

    @Enumerated(EnumType.STRING)
    private ProjectRole role;  // 역할

    @Column(name = "reporting_cnt", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int reportingCount;  // 신고 누적

    @Column(nullable = false)
    private String recruitCategory;  // 프로젝트 팀에 지원 시 신청자가 선택하는 모집 구분

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // 사용자 ID (주인)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectTeam projectTeam;  // 프로젝트 팀 ID (주인)

    @OneToMany(mappedBy = "projectParticipation")
    private List<Report> reports = new ArrayList<>();  // 신고 테이블과 일대다


    public static ProjectParticipation create(User user, ProjectTeam team) {
        ProjectParticipation projectParticipation = new ProjectParticipation();
        projectParticipation.participationStatus = ParticipationStatus.ACCEPTED;
        projectParticipation.isDeleted = false;
        projectParticipation.isExport = false;
        projectParticipation.requestDate = LocalDateTime.now();
        projectParticipation.decisionDate = LocalDateTime.now();
        projectParticipation.role = ProjectRole.OWNER;
        projectParticipation.recruitCategory = "OWNER";
        projectParticipation.user = user;
        projectParticipation.projectTeam = team;
        return projectParticipation;
    }

    public void joinTeamMember(User user, ProjectTeam projectTeam, String recruitCategory) {
        this.participationStatus = ParticipationStatus.PENDING;
        this.isDeleted = false;
        this.isExport = false;
        this.requestDate = LocalDateTime.now();
        this.role = ProjectRole.MEMBER;
        this.recruitCategory = recruitCategory;
        this.user = user;
        this.projectTeam = projectTeam;
    }

    // 탈퇴 가능 여부 확인
    public boolean canQuit() {
        return this.participationStatus == ParticipationStatus.ACCEPTED && !this.isDeleted;
    }

    // 탈퇴 처리
    public void quitTeam() {
        if (!canQuit()) {
            throw new BusinessException(ErrorCode.CANNOT_QUIT_TEAM);
        }
        this.isDeleted = true;
    }

    public boolean canAccept() {
        return this.participationStatus == ParticipationStatus.PENDING && !this.isDeleted;
    }

    public void acceptTeam() {
        if (!canAccept()) {
            throw new BusinessException(ErrorCode.CANNOT_ACCEPT_MEMBER);
        }
        this.participationStatus = ParticipationStatus.ACCEPTED;
        this.decisionDate = LocalDateTime.now();
    }

    public boolean canReject() {
        return this.participationStatus == ParticipationStatus.PENDING && !this.isDeleted;
    }

    public void rejectTeam() {
        if (!canReject()) {
            throw new BusinessException(ErrorCode.CANNOT_REJECT_MEMBER);
        }
        this.participationStatus = ParticipationStatus.REJECTED;
        this.decisionDate = LocalDateTime.now();
    }

    public void exportTeam() {
        this.isExport = true;
    }

    public void setRole(ProjectRole role) {
        this.role = role;
    }

    public void updateOwnerRole() {
        this.role = ProjectRole.MEMBER;
    }
}