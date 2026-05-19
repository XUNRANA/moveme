package com.moveme.module.seed.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("import_logs")
public class ImportLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String source;
    private String filePath;
    private Integer moviesTotal;
    private Integer moviesOk;
    private Integer moviesFail;
    private Integer personsOk;
    private Integer commentsOk;
    /** JSON 列：失败明细 */
    private String errors;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
