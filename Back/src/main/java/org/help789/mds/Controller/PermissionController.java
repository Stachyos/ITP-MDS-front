package org.help789.mds.Controller;

import org.help789.mds.Service.PermissionService;
import org.help789.mds.Entity.Vo.PermissionVo;
import org.help789.mds.logging.LogAction;   // ✅ 引入自定义注解
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions") // ✅ 带 api 前缀
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    /**
     * 获取所有用户的权限（包含账号和邮箱）
     */
    @LogAction(
            value = "permission:list",
            detail = "scope=all"
    )
    @GetMapping("/list")
    public List<PermissionVo> listPermissions() {
        return permissionService.getAllPermissions();
    }

    /**
     * 根据用户ID获取权限（包含账号和邮箱）
     */
    @LogAction(
            value = "permission:get",
            detail = "userId=#{#userId}"
    )
    @GetMapping("/{userId}")
    public PermissionVo getPermissionByUserId(@PathVariable Long userId) {
        return permissionService.getPermissionByUserId(userId);
    }

    /**
     * 新增或更新权限
     */
    @LogAction(
            value = "permission:save",
            // ⚠️ 若 PermissionVo 无这些字段，也不会抛错（SpEL 空安全 + 默认值），实在拿不到就记录 payload
            detail = "userId=#{#permissionVo?.userId?:-1},account=#{#permissionVo?.account?:'-'},email=#{#permissionVo?.email?:'-'},payload=#{#permissionVo}"
    )
    @PostMapping("/save")
    public PermissionVo savePermission(@RequestBody PermissionVo permissionVo) {
        return permissionService.savePermission(permissionVo);
    }

    /**
     * 删除权限记录
     */
    @LogAction(
            value = "permission:delete",
            detail = "permissionId=#{#permissionId}"
    )
    @DeleteMapping("/delete/{permissionId}")
    public String deletePermission(@PathVariable Long permissionId) {
        permissionService.deletePermission(permissionId);
        return "删除成功";
    }
}
