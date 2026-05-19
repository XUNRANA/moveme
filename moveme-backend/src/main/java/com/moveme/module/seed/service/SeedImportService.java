package com.moveme.module.seed.service;

import com.moveme.module.seed.dto.SeedMovieDTO;
import com.moveme.module.seed.support.ImportContext;
import com.moveme.module.seed.support.ImportResult;

public interface SeedImportService {

    /**
     * 全量导入 top250.json。失败一部不影响其他部，错误写入 import_logs.errors。
     * 由 SeedAutoRunner 在启动时按开关触发，或测试 / 管理端手动触发。
     */
    ImportResult importAll();

    /**
     * 单部导入。**调用方必须在事务内调用** —— 实现里没有自己开事务。
     * 一般通过 TransactionTemplate.execute(...) 包一层 REQUIRES_NEW 调用进来。
     */
    void importOne(SeedMovieDTO dto, ImportContext ctx);

    /**
     * 全量重导（清空除 users/字典外的所有业务数据再调 importAll）。慎用。
     */
    ImportResult reimport();
}
