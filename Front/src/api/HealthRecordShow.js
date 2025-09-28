import request from '@/utils/MDS.js'

// 获取全部
export function getAllHealthRecords() {
    return request.get(`/api/HealthRecordShow/getAll`)
}

// 新增
export function createHealthRecord(data) {
    return request.post(
        `/api/HealthRecordShow/create`,
        JSON.stringify(data),
        { headers: { 'Content-Type': 'application/json' } }
    )
}

// 更新
export function updateHealthRecord(id, data) {
    return request.put(
        `/api/HealthRecordShow/update/${id}`,
        JSON.stringify(data),
        { headers: { 'Content-Type': 'application/json' } }
    )
}

// 删除
export function deleteHealthRecord(id) {
    return request.delete(`/api/HealthRecordShow/delete/${id}`)
}

// 导出excel
export function downloadHealthRecordTemplate() {
    return request
        .get('/api/HealthRecordShow/downloadTemplate', { responseType: 'blob' })
        .then(res => res.data ?? res)  // 统一成 Blob
}

// 导入数据（CSV/Excel/JSON）— 关闭超时（默认 timeout: 0），并允许外部透传 axios 配置
export function importHealthRecords(file, format, extraCfg = {}) {
    const form = new FormData();
    form.append('file', file);
    if (format) form.append('format', format);

    // 处理 headers：删除显式 Content-Type，交给浏览器按 FormData 自动带 boundary
    const mergedHeaders = { ...(extraCfg.headers || {}) };
    delete mergedHeaders['Content-Type'];
    delete mergedHeaders['content-type'];

    return request.post('/api/HealthRecordShow/import', form, {
        // 默认不超时；如需自定义，可在调用处传入 { timeout: xxx }
        timeout: 0,

        // 先合入外部配置，再用我们处理过的 headers 覆盖，确保不写死 Content-Type
        ...extraCfg,
        headers: mergedHeaders,

        // 兼容 axios v1：如果 setContentType 存在，把它置空；并保留调用方自定义的 transformRequest
        transformRequest: [
            ...(extraCfg.transformRequest || []),
            (data, headers) => {
                headers?.setContentType?.(undefined);
                return data;
            }
        ]
    });
}