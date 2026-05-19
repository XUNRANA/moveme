package com.moveme.module.movie.controller;

import com.moveme.common.result.Result;
import com.moveme.module.movie.service.PersonService;
import com.moveme.module.movie.vo.PersonDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "人物模块")
@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @Operation(summary = "人物详情")
    @GetMapping("/{id}")
    public Result<PersonDetailVO> getPersonDetail(@PathVariable Long id) {
        PersonDetailVO vo = personService.getPersonDetail(id);
        if (vo == null) {
            return Result.error(404, "人物不存在");
        }
        return Result.success(vo);
    }
}
