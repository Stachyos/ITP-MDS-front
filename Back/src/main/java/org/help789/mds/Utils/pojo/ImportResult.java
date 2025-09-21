package org.help789.mds.Utils.pojo;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResult {
    private int totalRows;          // 原始行数
    private int skippedMissingKey;  // 因关键字段缺失（如 name）跳过
    private int skippedAbnormal;    // 因异常值过滤跳过
    private int deduplicated;       // 去重丢弃的条数
    private int saved;              // 最终入库条数
    private String message;         // 备注信息
}
