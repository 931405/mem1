package com.memosystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 版本信息 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionInfoVO {
    private String version;
    private String description;
    private String buildDate;
}
