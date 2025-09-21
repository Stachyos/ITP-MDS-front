package org.help789.mds.Service;

import org.help789.mds.Entity.Vo.HealthRecordShowVo;

import java.io.OutputStream;
import java.util.List;

public interface HealthRecordShowService {

    /** 展示全部 */
    List<HealthRecordShowVo> listAll();

    /** 新增 */
    HealthRecordShowVo create(HealthRecordShowVo vo);

    /** 修改（按ID覆盖式更新） */
    HealthRecordShowVo update(Long recordId, HealthRecordShowVo vo);

    /** 删除单条 */
    void delete(Long recordId);

    /** 导出填写模板 */
    void writeTemplate(OutputStream out);
}
