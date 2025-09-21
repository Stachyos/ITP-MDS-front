import request from '@/utils/MDS.js'

// 获取全部
export function getAllHealthRecords() {
    return request.get(`/HealthRecordShow/getAll`)
}

// 新增
export function createHealthRecord(data) {
    return request.post(
        `/HealthRecordShow/create`,
        JSON.stringify(data),
        { headers: { 'Content-Type': 'application/json' } }
    )
}

// 更新
export function updateHealthRecord(id, data) {
    return request.put(
        `/HealthRecordShow/update/${id}`,
        JSON.stringify(data),
        { headers: { 'Content-Type': 'application/json' } }
    )
}

// 删除
export function deleteHealthRecord(id) {
    return request.delete(`/HealthRecordShow/delete/${id}`)
}

// 导出excel
export function downloadHealthRecordTemplate() {
    return request
        .get('/HealthRecordShow/downloadTemplate', { responseType: 'blob' })
        .then(res => res.data ?? res)  // 统一成 Blob
}

// 导入数据（CSV/Excel/JSON）
export function importHealthRecords(file, format) {
    const form = new FormData()
    form.append('file', file)
    const url = format ? `/HealthRecordShow/import?format=${format}` : `/HealthRecordShow/import`
    return request.post(url, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000,                // ← 单接口加长
        onUploadProgress: e => {/* 可选：显示进度 */}
    })
}