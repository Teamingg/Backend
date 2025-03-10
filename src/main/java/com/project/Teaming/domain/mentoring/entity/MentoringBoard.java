package com.project.Teaming.domain.mentoring.entity;

import com.project.Teaming.domain.mentoring.dto.request.BoardRequest;
import com.project.Teaming.domain.mentoring.dto.response.BoardSpecResponse;
import com.project.Teaming.global.auditing.BaseTimeEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "mentoring_board")
@NoArgsConstructor
public class MentoringBoard extends BaseTimeEntity {
    @Id
    @Tsid
    @Column(name = "mentoring_board_id")
    private Long id;  // 멘토링 모집글 ID

    @Column(nullable = false, length = 100)
    private String title;  // 모집글 제목

    @Column(columnDefinition = "TEXT")
    private String contents;  // 모집글 내용

    @Enumerated(EnumType.STRING)
    private MentoringRole role;  // 모집하는 역할, 팀과 컬럼의 역할이 다름

    @Column(name = "mentoring_dead_line")
    private LocalDate deadLine;

    @Enumerated(EnumType.STRING)
    private PostStatus status;

    @Column
    private Integer mentoringCnt;  //수정할 수 있도록 팀과 칼럼중복

    @Column(name = "link", length = 1000)
    private String link;  // 연락 방법

    // 멘토링 팀 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentoring_team_id")
    private MentoringTeam mentoringTeam;  // 멘토링 팀 ID (주인)

    @Version
    private Integer version;


    public MentoringBoard(Long id, String title, String contents, MentoringRole role, LocalDate deadLine, PostStatus status, Integer mentoringCnt, String link, MentoringTeam mentoringTeam) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.role = role;
        this.deadLine = deadLine;
        this.status = status;
        this.mentoringCnt = mentoringCnt;
        this.link = link;
        this.mentoringTeam = mentoringTeam;
    }

    public MentoringBoard(String title, String contents, MentoringRole role, LocalDate deadLine, PostStatus status, Integer mentoringCnt) {
        this.title = title;
        this.contents = contents;
        this.role = role;
        this.deadLine = deadLine;
        this.status = status;
        this.mentoringCnt = mentoringCnt;
    }

    public static MentoringBoard from(BoardRequest request) {
        return new MentoringBoard(
                request.getTitle(),request.getContents(),request.getRole(),request.getDeadLine(),
                PostStatus.RECRUITING, request.getMentoringCnt());
    }

    public void link(String link) {
        this.link = link;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    public void updateStatus(){
        this.status = PostStatus.COMPLETE;
        this.deadLine = LocalDate.now().minusDays(1);
    }

    public BoardSpecResponse toDto(MentoringTeam mentoringTeam) {
        return BoardSpecResponse.from(this, mentoringTeam);
    }

    public void updateBoard(BoardRequest dto) {
        this.title = dto.getTitle();
        this.role = dto.getRole();
        this.mentoringCnt = dto.getMentoringCnt();
        this.link = dto.getLink();
        this.deadLine = dto.getDeadLine();
        this.contents = dto.getContents();
    }

    /**
     * 연관관계 편의 메서드
     */
    public void addMentoringBoard(MentoringTeam mentoringTeam) {
        this.mentoringTeam = mentoringTeam;
        mentoringTeam.getMentoringBoardList().add(this);
    }

    public void removeMentoringBoard(MentoringTeam mentoringTeam) {
        if (this.mentoringTeam != null) {
            this.mentoringTeam.getMentoringBoardList().remove(this);
            this.mentoringTeam = null;
        }
    }
}