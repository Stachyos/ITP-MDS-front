package org.help789.mds.Controller;

import lombok.RequiredArgsConstructor;
import org.help789.mds.Entity.vo.HealthRecordShow;
import org.help789.mds.Service.HealthRecordShowService;
import org.help789.mds.Utils.pojo.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/HealthRecordShow")
@RequiredArgsConstructor
public class HealthRecordShowController {

    /** 注意：要用 final，配合 @RequiredArgsConstructor 才会注入 */
    private final HealthRecordShowService healthRecordService;

    /** 展示全部 => GET /api/HealthRecordShow/getAll */
    @GetMapping("/getAll")
    public Result<List<HealthRecordShow>> getAll() {
        try {
            List<HealthRecordShow> list = healthRecordService.listAll();
            return Result.success(list);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /** 新增 => POST /api/HealthRecordShow/create */
    @PostMapping("/create")
    public Result<HealthRecordShow> create(@RequestBody HealthRecordShow body) {
        try {
            HealthRecordShow created = healthRecordService.create(body);
            return Result.success("created", created);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /** 修改（按ID覆盖更新） => PUT /api/HealthRecordShow/update/{id} */
    @PutMapping("/update/{id}")
    public Result<HealthRecordShow> update(@PathVariable("id") Long id,
                                           @RequestBody HealthRecordShow body) {
        try {
            body.setRecordId(id); // 以路径ID为准
            HealthRecordShow updated = healthRecordService.update(id, body);
            return Result.success("updated", updated);
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }

    /** 删除 => DELETE /api/HealthRecordShow/delete/{id} */
    @DeleteMapping("/delete/{id}")
    public Result<Long> delete(@PathVariable("id") Long id) {
        try {
            healthRecordService.delete(id);
            return Result.success("deleted", id); // 返回被删除的ID
        } catch (Exception e) {
            return Result.failed(e.getMessage());
        }
    }
}
