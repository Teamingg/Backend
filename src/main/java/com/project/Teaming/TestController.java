package com.project.Teaming;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Test", description = "Test Controller 입니다.")
public class TestController {

    @GetMapping("/")
    @Operation(summary = "home", description = "/ home")
    public String home() {
        return "Hello Teaming";
    }

    @GetMapping("/health")
    @Operation(summary = "헬스체크 컨트롤러")
    public String health() {
        return "OK";
    }
}
