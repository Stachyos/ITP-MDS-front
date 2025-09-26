import request from '@/utils/MDS.js'

export function getAuditLogs(page = 0, size = 10) {
    return request({
        url: '/api/auditLog/list',
        method: 'get',
        params: { page, size }
    })
}
