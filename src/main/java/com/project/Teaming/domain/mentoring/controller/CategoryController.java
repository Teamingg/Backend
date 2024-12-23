package com.project.Teaming.domain.mentoring.controller;

import com.project.Teaming.domain.mentoring.dto.response.MentoringCategoryDto;
import com.project.Teaming.domain.mentoring.service.CategoryService;
import com.project.Teaming.global.result.ResultCode;
import com.project.Teaming.global.result.ResultDetailResponse;
import com.project.Teaming.global.result.ResultListResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/mentoring")
@Tag(name = "MentoringCategory", description = "멘토링 모집 카테고리 관련 API")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/category/{category_id}")
    public ResultDetailResponse<MentoringCategoryDto> findCategory(@PathVariable Long category_id) {
        MentoringCategoryDto category = categoryService.findCategory(category_id);
        return new ResultDetailResponse<>(ResultCode.FIND_MENTORING_CATEGORY, category);
    }

    @GetMapping("/category")
    public ResultListResponse<MentoringCategoryDto> findAllCategory() {
        List<MentoringCategoryDto> allCategory = categoryService.findAllCategory();
        return new ResultListResponse<>(ResultCode.FIND_ALL_MENTORING_CATEGORY, allCategory);
    }
}