package org.help789.mds.Controller;

import lombok.RequiredArgsConstructor;
import org.help789.mds.Entity.AuditLog;
import org.help789.mds.Utils.pojo.Result;
import org.help789.mds.repository.AuditLogRepository;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auditLog")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository repo;

    /**
     * 分页查询审计日志
     * 例：GET /api/auditLog/list?page=0&size=10
     */
    @GetMapping("/list")
    public Result<Page<AuditLog>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "time"));
        Page<AuditLog> logs = repo.findAll(pageable);
        return Result.success(logs);
    }

    /**
     * 按用户ID查询日志
     * 例：GET /api/auditLog/user/123?page=0&size=10
     */
    @GetMapping("/user/{userId}")
    public Result<Page<AuditLog>> listByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "time"));
        Page<AuditLog> logs = repo.findAll(
                (root, query, cb) -> cb.equal(root.get("userId"), userId),
                pageable
        );
        return Result.success(logs);
    }

    /**
     * 查看单条日志详情
     * 例：GET /api/auditLog/1
     */
    @GetMapping("/{id}")
    public Result<AuditLog> getById(@PathVariable Long id) {
        return repo.findById(id)
                .map(Result::success)
                .orElseGet(() -> Result.failed("日志不存在"));
    }
}
