package com.project.Teaming.domain.project.service;

import com.project.Teaming.domain.project.dto.request.CreateTeamDto;
import com.project.Teaming.domain.project.dto.request.UpdateTeamDto;
import com.project.Teaming.domain.project.dto.response.ProjectTeamInfoDto;
import com.project.Teaming.domain.project.entity.ProjectTeam;
import com.project.Teaming.domain.project.entity.RecruitCategory;
import com.project.Teaming.domain.project.entity.Stack;
import com.project.Teaming.domain.project.entity.TeamRecruitCategory;
import com.project.Teaming.domain.project.entity.TeamStack;
import com.project.Teaming.domain.project.repository.ProjectTeamRepository;
import com.project.Teaming.domain.project.repository.RecruitCategoryRepository;
import com.project.Teaming.domain.project.repository.StackRepository;
import com.project.Teaming.domain.project.repository.TeamRecruitCategoryRepository;
import com.project.Teaming.domain.project.repository.TeamStackRepository;
import com.project.Teaming.global.error.ErrorCode;
import com.project.Teaming.global.error.exception.BusinessException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProjectTeamService {

    private final ProjectTeamRepository projectTeamRepository;
    private final StackRepository stackRepository;
    private final TeamStackRepository teamStackRepository;
    private final RecruitCategoryRepository recruitCategoryRepository;
    private final TeamRecruitCategoryRepository teamRecruitCategoryRepository;

    public ProjectTeam createTeam(CreateTeamDto dto) {
        ProjectTeam projectTeam = ProjectTeam.projectTeam(dto);
        projectTeamRepository.save(projectTeam);

        List<Long> stackIds = dto.getStackIds();
        List<Stack> stacks = stackRepository.findAllById(stackIds);

        // 누락된 기술 스택 ID 검증
        List<Long> missingStackIds = stackIds.stream()
                .filter(id -> stacks.stream().noneMatch(stack -> stack.getId().equals(id)))
                .collect(Collectors.toList());
        if (!missingStackIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_VALID_STACK_ID);
        }

        for (Stack stack : stacks) {
            TeamStack teamStack = TeamStack.addStacks(projectTeam, stack);
            teamStackRepository.save(teamStack);
        }

        List<Long> recruitCategoryIds = dto.getRecruitCategoryIds();
        List<RecruitCategory> recruitCategories = recruitCategoryRepository.findAllById(recruitCategoryIds);

        // 누락된 모집 카테고리 ID 검증
        List<Long> missingRecruitCategoryIds = recruitCategoryIds.stream()
                .filter(id -> recruitCategories.stream().noneMatch(category -> category.getId().equals(id)))
                .collect(Collectors.toList());
        if (!missingRecruitCategoryIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_VALID_RECRUIT_CATEGORY_ID);
        }

        for (RecruitCategory recruitCategory : recruitCategories) {
            TeamRecruitCategory teamRecruitCategory = TeamRecruitCategory.addRecruitCategories(projectTeam, recruitCategory);
            teamRecruitCategoryRepository.save(teamRecruitCategory);
        }

        return projectTeam;
    }

    public ProjectTeamInfoDto getTeam(Long teamId) {
        ProjectTeam projectTeam = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_TEAM));

        // 기술 스택 id 리스트 생성
        List<Long> stackIds = projectTeam.getStacks().stream()
                .map(teamStack -> teamStack.getStack().getId())
                .collect(Collectors.toList());

        // 모집 구분 id 리스트 생성
        List<Long> recruitCategoryIds = projectTeam.getRecruitCategories().stream()
                .map(teamRecruitCategory -> teamRecruitCategory.getRecruitCategory().getId())
                .collect(Collectors.toList());

        return getProjectTeamInfoDto(projectTeam, stackIds, recruitCategoryIds);
    }

    private static ProjectTeamInfoDto getProjectTeamInfoDto(ProjectTeam projectTeam, List<Long> stackIds, List<Long> recruitCategoryIds) {
        ProjectTeamInfoDto dto = new ProjectTeamInfoDto();
        dto.setProjectId(projectTeam.getId());
        dto.setProjectName(projectTeam.getName());
        dto.setStartDate(projectTeam.getStartDate());
        dto.setEndDate(projectTeam.getEndDate());
        dto.setDeadline(projectTeam.getDeadline());
        dto.setMemberCnt(projectTeam.getMembersCnt());
        dto.setLink(projectTeam.getLink());
        dto.setContents(projectTeam.getContents());
        dto.setCreatedDate(projectTeam.getCreatedDate());
        dto.setLastModifiedDate(projectTeam.getLastModifiedDate());
        dto.setStacks(stackIds);
        dto.setRecruitCategories(recruitCategoryIds);
        return dto;
    }

    public void editTeam(Long teamId, UpdateTeamDto dto) {
        ProjectTeam projectTeam = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_TEAM));

        List<Stack> stacks = stackRepository.findAllById(dto.getStackIds());
        List<Long> missingStackIds = dto.getStackIds().stream()
                .filter(id -> stacks.stream().noneMatch(stack -> stack.getId().equals(id)))
                .collect(Collectors.toList());
        if (!missingStackIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_VALID_STACK_ID);
        }

        List<RecruitCategory> recruitCategories = recruitCategoryRepository.findAllById(dto.getRecruitCategoryIds());
        List<Long> missingCategoryIds = dto.getRecruitCategoryIds().stream()
                .filter(id -> recruitCategories.stream().noneMatch(category -> category.getId().equals(id)))
                .collect(Collectors.toList());
        if (!missingCategoryIds.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_VALID_RECRUIT_CATEGORY_ID);
        }

        projectTeam.updateProjectTeam(dto);
        projectTeam.updateStacks(stacks);
        projectTeam.updateRecruitCategories(recruitCategories);
    }

    public void deleteTeam(Long teamId) {
        ProjectTeam projectTeam = projectTeamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PROJECT_TEAM));

        projectTeamRepository.delete(projectTeam);
    }
}
