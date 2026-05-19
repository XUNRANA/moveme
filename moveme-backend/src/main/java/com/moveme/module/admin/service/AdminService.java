package com.moveme.module.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moveme.module.crawler.entity.CrawlLog;
import com.moveme.module.seed.entity.ImportLog;
import com.moveme.module.user.entity.RecommendationLog;
import com.moveme.module.admin.vo.AdminStatsVO;
import com.moveme.module.admin.vo.AdminUserVO;

public interface AdminService {

    AdminStatsVO getStats();

    IPage<AdminUserVO> listUsers(String keyword, Integer role, Integer status, long page, long size);

    void updateUserStatus(Long userId, Integer status);

    void updateUserRole(Long userId, Integer role);

    IPage<CrawlLog> listCrawlLogs(long page, long size);

    IPage<ImportLog> listImportLogs(long page, long size);

    IPage<RecommendationLog> listRecoLogs(long page, long size);
}
