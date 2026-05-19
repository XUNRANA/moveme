package com.moveme.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moveme.common.result.Result;
import com.moveme.module.admin.service.AdminService;
import com.moveme.module.crawler.entity.CrawlLog;
import com.moveme.module.seed.entity.ImportLog;
import com.moveme.module.user.entity.RecommendationLog;
import com.moveme.module.admin.vo.AdminStatsVO;
import com.moveme.module.admin.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员模块")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "获取系统统计")
    @GetMapping("/stats")
    public Result<AdminStatsVO> getStats() {
        return Result.success(adminService.getStats());
    }

    @Operation(summary = "获取用户列表")
    @GetMapping("/users")
    public Result<IPage<AdminUserVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.listUsers(keyword, role, status, page, size));
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.updateUserStatus(id, body.get("status"));
        return Result.success();
    }

    @Operation(summary = "更新用户角色")
    @PutMapping("/users/{id}/role")
    public Result<Void> updateUserRole(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.updateUserRole(id, body.get("role"));
        return Result.success();
    }

    @Operation(summary = "获取爬取日志")
    @GetMapping("/crawl-logs")
    public Result<IPage<CrawlLog>> listCrawlLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.listCrawlLogs(page, size));
    }

    @Operation(summary = "获取导入日志")
    @GetMapping("/import-logs")
    public Result<IPage<ImportLog>> listImportLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.listImportLogs(page, size));
    }

    @Operation(summary = "获取推荐日志")
    @GetMapping("/reco-logs")
    public Result<IPage<RecommendationLog>> listRecoLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminService.listRecoLogs(page, size));
    }
}
