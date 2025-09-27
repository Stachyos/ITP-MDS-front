import { buildOneRow, buildFileBase } from './common.js'
import { ElMessage } from 'element-plus'

export const exportRecordCSV = (row) => {
    const r = buildOneRow(row)
    const headers = Object.keys(r)
    const esc = (s) => {
        const str = String(s ?? '')
        return /[",\n]/.test(str) ? `"${str.replace(/"/g, '""')}"` : str
    }

    const lines = [
        headers.join(','),
        headers.map(h => esc(r[h])).join(',')
    ]
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${buildFileBase(row)}.csv`
    document.body.appendChild(a); a.click(); document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('单条 CSV 导出完成')
}
