import request from '@/utils/MDS.js'

// 获取全部权限
export function getAllPermissions() {
    return request.get(`/api/permissions/list`)
}

// 根据用户ID获取权限
export function getPermissionByUserId(userId) {
    return request.get(`/api/permissions/${userId}`)
}

// 保存或更新权限
export function savePermission(data) {
    return request.post(
        `/api/permissions/save`,
        JSON.stringify(data),
        { headers: { 'Content-Type': 'application/json' } }
    )
}

// 删除权限
export function deletePermission(permissionId) {
    return request.delete(`/api/permissions/delete/${permissionId}`)
}
