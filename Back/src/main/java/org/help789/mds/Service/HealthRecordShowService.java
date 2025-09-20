package org.help789.mds.Service;

import org.help789.mds.Entity.vo.HealthRecordShow;

import java.util.List;

public interface HealthRecordShowService {

    /** 展示全部 */
    List<HealthRecordShow> listAll();

    /** 新增 */
    HealthRecordShow create(HealthRecordShow vo);

    /** 修改（按ID覆盖式更新） */
    HealthRecordShow update(Long recordId, HealthRecordShow vo);

    /** 删除单条 */
    void delete(Long recordId);
}
