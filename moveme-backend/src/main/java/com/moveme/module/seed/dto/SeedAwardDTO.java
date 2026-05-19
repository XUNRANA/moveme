package com.moveme.module.seed.dto;

import lombok.Data;

import java.util.List;

@Data
public class SeedAwardDTO {
    /** "第67届奥斯卡金像奖" */
    private String name;
    private String url;
    /** "最佳影片" */
    private String category;
    /** "won" / "nominated" / "unknown" */
    private String status;
    private List<SeedPersonRefDTO> recipients;
}
