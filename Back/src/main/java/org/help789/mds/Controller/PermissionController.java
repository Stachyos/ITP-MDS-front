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

    /** Get permissions for all users */
    @LogAction(
            value = "permission:list",
            detail = "View permissions list (all)"
    )
    @GetMapping("/list")
    public List<PermissionVo> listPermissions() {
        return permissionService.getAllPermissions();
    }

    /** Get permissions by user ID */
    @LogAction(
            value = "permission:get",
            detail = "View user permissions: userId=#{#userId}"
    )
    @GetMapping("/{userId}")
    public PermissionVo getPermissionByUserId(@PathVariable Long userId) {
        System.out.println("PermissionController.getPermissionByUserId: " + permissionService.getPermissionByUserId(userId));
        return permissionService.getPermissionByUserId(userId);
    }

    /** Create or update permissions */
    @LogAction(
            value = "permission:save",
            detail = "Save user permissions: userId=#{#permissionVo?.userId?:-1}, account=#{#permissionVo?.account?:'-'}, email=#{#permissionVo?.email?:'-'}"
    )
    @PostMapping("/save")
    public PermissionVo savePermission(@RequestBody PermissionVo permissionVo) {
        return permissionService.savePermission(permissionVo);
    }

    /** Delete a permission record */
    @LogAction(
            value = "permission:delete",
            detail = "Delete permission record: permissionId=#{#permissionId}"
    )
    @DeleteMapping("/delete/{permissionId}")
    public String deletePermission(@PathVariable Long permissionId) {
        permissionService.deletePermission(permissionId);
        return "Deleted successfully";
    }
}
