package org.help789.mds.Controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.help789.mds.Entity.Vo.HealthRecordShowVo;
import org.help789.mds.Service.HealthRecordShowService;
import org.help789.mds.Utils.pojo.ImportResult;
import org.help789.mds.Utils.pojo.Result;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/HealthRecordShow")
@RequiredArgsConstructor
public class HealthRecordShowController {

    /** 注意：要用 final，配合 @RequiredArgsConstructor 才会注入 */
    private final HealthRecordShowService healthRecordService;

    /** 展示全部 => GET /HealthRecordShow/getAll */
    @GetMapping("/getAll")
    public Result<List<HealthRecordShowVo>> getAll() {
        try {
            List<HealthRecordShowVo> list = healthRecordService.listAll();
            return Result.success(list);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /** 新增 => POST /HealthRecordShow/create */
    @PostMapping("/create")
    public Result<HealthRecordShowVo> create(@RequestBody HealthRecordShowVo body) {
        try {
            HealthRecordShowVo created = healthRecordService.create(body);
            return Result.success("created", created);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /** 修改（按ID覆盖更新） => PUT /HealthRecordShow/update/{id} */
    @PutMapping("/update/{id}")
    public Result<HealthRecordShowVo> update(@PathVariable("id") Long id,
                                             @RequestBody HealthRecordShowVo body) {
        try {
            body.setRecordId(id); // 以路径ID为准
            HealthRecordShowVo updated = healthRecordService.update(id, body);
            return Result.success("updated", updated);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /** 删除 => DELETE /HealthRecordShow/delete/{id} */
    @DeleteMapping("/delete/{id}")
    public Result<Long> delete(@PathVariable("id") Long id) {
        try {
            healthRecordService.delete(id);
            return Result.success("deleted", id); // 返回被删除的ID
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /**
     * 下载健康数据 Excel 模板
     * GET /api/HealthRecordShow/downloadTemplate
     */
    @GetMapping("/downloadTemplate")
    public void downloadTemplate(HttpServletResponse response) {
        String filename = "健康数据模板.xlsx";
        try {
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            // 兼容中文文件名
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encoded);

            healthRecordService.writeTemplate(response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            // 如果需要返回 JSON 错误，可以在这里重置响应并写入 Result，但下载场景通常直接抛错即可。
            throw new RuntimeException("模板下载失败", e);
        }
    }

    /**
     * 批量导入：支持 CSV / Excel / JSON
     * 例：POST /api/HealthRecordShow/import?format=excel
     * form-data: file=<上传文件>
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImportResult> importData(@RequestPart("file") MultipartFile file,
                                           @RequestParam(value = "format", required = false) String format) {
        try {
            ImportResult result = healthRecordService.importData(file, format);
            return Result.success("导入成功", result);
        } catch (Exception e) {
            return Result.failed("导入失败：" + e.getMessage());
        }
    }
}
