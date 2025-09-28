package org.help789.mds.Controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.help789.mds.Entity.Vo.HealthRecordShowVo;
import org.help789.mds.Service.HealthRecordShowService;
import org.help789.mds.Utils.pojo.ImportResult;
import org.help789.mds.Utils.pojo.Result;
import org.help789.mds.logging.LogAction;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/HealthRecordShow")
@RequiredArgsConstructor
public class HealthRecordShowController {

    private final HealthRecordShowService healthRecordService;

    /** List all => GET /HealthRecordShow/getAll */
    @LogAction(
            value = "health-record:list",
            detail = "View health data list (all)"
    )
    @GetMapping("/getAll")
    public Result<List<HealthRecordShowVo>> getAll() {
        try {
            List<HealthRecordShowVo> list = healthRecordService.listAll();
            return Result.success(list);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /** Create => POST /HealthRecordShow/create */
    @LogAction(
            value = "health-record:create",
            detail = "Create health record: name=#{#body?.name?:'-'}, sex=#{#body?.sex?:'Unknown'}, age=#{#body?.age?:-1}"
    )
    @PostMapping("/create")
    public Result<HealthRecordShowVo> create(@RequestBody HealthRecordShowVo body) {
        try {
            HealthRecordShowVo created = healthRecordService.create(body);
            return Result.success("created", created);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /** Update (overwrite by ID) => PUT /HealthRecordShow/update/{id} */
    @LogAction(
            value = "health-record:update",
            detail = "Update health record: ID=#{#id}, name=#{#body?.name?:'-'}, age=#{#body?.age?:-1}"
    )
    @PutMapping("/update/{id}")
    public Result<HealthRecordShowVo> update(@PathVariable("id") Long id,
                                             @RequestBody HealthRecordShowVo body) {
        try {
            body.setRecordId(id); // Use path ID as the source of truth
            HealthRecordShowVo updated = healthRecordService.update(id, body);
            return Result.success("updated", updated);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /** Delete => DELETE /HealthRecordShow/delete/{id} */
    @LogAction(
            value = "health-record:delete",
            detail = "Delete health record: ID=#{#id}"
    )
    @DeleteMapping("/delete/{id}")
    public Result<Long> delete(@PathVariable("id") Long id) {
        try {
            healthRecordService.delete(id);
            return Result.success("deleted", id);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /** Download Excel template => GET /api/HealthRecordShow/downloadTemplate */
    @LogAction(
            value = "health-record:download-template",
            detail = "Download health data template: filename=HealthDataTemplate.xlsx"
    )
    @GetMapping("/downloadTemplate")
    public void downloadTemplate(HttpServletResponse response) {
        String filename = "HealthDataTemplate.xlsx";
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);

            healthRecordService.writeTemplate(response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            throw new RuntimeException("Template download failed", e);
        }
    }

    /** Bulk import => POST /api/HealthRecordShow/import */
    @LogAction(
            value = "health-record:import",
            detail = "Bulk import health data: file=#{#file?.originalFilename?:'-'}, size=#{#file?.size?:0}B, format=#{#format?:'auto'}"
    )
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImportResult> importData(@RequestPart("file") MultipartFile file,
                                           @RequestParam(value = "format", required = false) String format) {
        try {
            ImportResult result = healthRecordService.importData(file, format);
            return Result.success("Import succeeded", result);
        } catch (Exception e) {
            return Result.failed("Import failed: " + e.getMessage());
        }
    }
}
