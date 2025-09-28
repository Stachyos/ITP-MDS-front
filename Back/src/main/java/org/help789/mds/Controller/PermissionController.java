package org.help789.mds.Controller;

import org.help789.mds.Service.PermissionService;
import org.help789.mds.Entity.Vo.PermissionVo;
import org.help789.mds.logging.LogAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    /** 获取所有用户的权限 */
    @LogAction(
            value = "permission:list",
            detail = "查看权限列表（全部）"
    )
    @GetMapping("/list")
    public List<PermissionVo> listPermissions() {
        return permissionService.getAllPermissions();
    }

    /** 根据用户ID获取权限 */
    @LogAction(
            value = "permission:get",
            detail = "查看用户权限：userId=#{#userId}"
    )
    @GetMapping("/{userId}")
    public PermissionVo getPermissionByUserId(@PathVariable Long userId) {
        System.out.println("PermissionController.getPermissionByUserId: " + permissionService.getPermissionByUserId(userId));
        return permissionService.getPermissionByUserId(userId);
    }

    /** 新增或更新权限 */
    @LogAction(
            value = "permission:save",
            detail = "保存用户权限：userId=#{#permissionVo?.userId?:-1}，账号=#{#permissionVo?.account?:'-'}，邮箱=#{#permissionVo?.email?:'-'}"
    )
    @PostMapping("/save")
    public PermissionVo savePermission(@RequestBody PermissionVo permissionVo) {
        return permissionService.savePermission(permissionVo);
    }

    /** 删除权限记录 */
    @LogAction(
            value = "permission:delete",
            detail = "删除权限记录：permissionId=#{#permissionId}"
    )
    @DeleteMapping("/delete/{permissionId}")
    public String deletePermission(@PathVariable Long permissionId) {
        permissionService.deletePermission(permissionId);
        return "删除成功";
    }
}
