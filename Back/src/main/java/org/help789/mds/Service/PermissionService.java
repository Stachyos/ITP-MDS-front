package org.help789.mds.Service;

import org.help789.mds.Entity.Vo.PermissionVo;

import java.util.List;

public interface PermissionService {

    /**
     * 获取所有用户的权限（包含账号和邮箱）
     */
    List<PermissionVo> getAllPermissions();

    /**
     * 根据用户ID获取权限（包含账号和邮箱）
     */
    PermissionVo getPermissionByUserId(Long userId);

    /**
     * 保存/更新某个用户的权限
     * - 保存前确保 userId 在 users 表存在
     * - 返回带 account/email 的 PermissionVo
     */
    PermissionVo savePermission(PermissionVo permissionVo);

    /**
     * 删除权限记录
     */
    void deletePermission(Long permissionId);
}