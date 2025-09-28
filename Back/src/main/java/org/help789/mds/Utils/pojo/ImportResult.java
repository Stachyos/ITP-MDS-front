package org.help789.mds.Utils.pojo;

import lombok.*;

/**
 * This class represents the result of an import operation, including the counts
 * of different scenarios like skipped rows, deduplicated rows, and successfully saved records.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResult {

    /**
     * 原始数据的总行数，包括所有未跳过的行。
     */
    private int totalRows;

    /**
     * 因关键字段缺失（如 name）而跳过的记录数。
     */
    private int skippedMissingKey;

    /**
     * 因异常值过滤（如数据格式错误、值不符合标准）而跳过的记录数。
     */
    private int skippedAbnormal;

    /**
     * 去重操作丢弃的记录数（可能是重复的记录）。
     */
    private int deduplicated;

    /**
     * 成功存入数据库的记录数。
     */
    private int saved;

    /**
     * 导入过程中的备注信息，可以是导入过程中产生的错误或成功的消息。
     */
    private String message;

    /**
     * 获取导入结果的简要总结，方便显示或日志记录。
     *
     * @return 返回一个格式化的字符串，包含总行数、跳过的记录数、去重记录数和保存的记录数。
     */
    public String getSummary() {
        return String.format("Total Rows: %d, Skipped Missing Keys: %d, Skipped Abnormal: %d, " +
                        "Deduplicated: %d, Saved: %d, Message: %s",
                totalRows, skippedMissingKey, skippedAbnormal, deduplicated, saved, message);
    }

    /**
     * 设置成功的记录数。
     * @param saved 成功保存的记录数
     */
    public void setSuccessCount(int saved) {
        this.saved = saved;  // 更新成功记录数
    }

    /**
     * 设置失败的记录数。
     * 失败记录数 = 跳过的关键字段缺失记录数 + 跳过的异常记录数 + 去重记录数
     * @param failureCount 失败的记录数
     */
    public void setFailureCount(int failureCount) {
        // 计算并设置失败的记录数
        this.skippedMissingKey = failureCount; // 设置跳过的记录数为失败记录数
        this.skippedAbnormal = failureCount;   // 如果失败是由于异常原因，你可以单独处理
        this.deduplicated = failureCount;      // 去重记录
    }
}
