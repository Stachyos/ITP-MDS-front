package org.help789.mds.Service;

import org.help789.mds.Entity.Vo.HealthRecordShowVo;
import org.help789.mds.Utils.pojo.ImportResult;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 批量导入健康数据。
     * @param file   上传文件（csv/xlsx/xls/json）
     * @param format 可选：csv|excel|json（为空则按文件后缀自动识别）
     * @return ImportResult 统计信息
     */
    ImportResult importData(MultipartFile file, String format);
}
