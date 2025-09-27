import { EXPORT_COLUMNS } from '@/constants/healthRecordExportColumns.js'

export const ts = () => {
    const d = new Date()
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}${pad(d.getMonth()+1)}${pad(d.getDate())}_${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
}

export const safe = (v) => (v ?? '') // 导出值兜底

// 单条：将一条记录映射成 { 标题: 值 }，内含列级 format
export const buildOneRow = (row) => {
    const obj = {}
    for (const c of EXPORT_COLUMNS) {
        const raw = row?.[c.key]
        obj[c.title] = typeof c.format === 'function' ? c.format(raw, row) : safe(raw)
    }
    return obj
}

// 统一文件名（含姓名+ID+时间戳）
export const buildFileBase = (row, prefix = 'HealthRecord') => {
    const name = (row?.name ?? 'one').toString().replace(/[\\/:*?"<>|]/g, '_')
    const id   = (row?.recordId ?? 'one').toString()
    return `${prefix}_${name}_${id}_${ts()}`
}
