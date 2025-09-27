import { buildOneRow, buildFileBase } from './common.js'
import { ElMessage } from 'element-plus'

export const exportRecordXLSX = async (row) => {
    try {
        const xlsx = await import('xlsx')
        const r = buildOneRow(row)
        const ws = xlsx.utils.json_to_sheet([r])

        // 自动列宽
        const headers = Object.keys(r)
        ws['!cols'] = headers.map((h) => {
            const contentLen = String(r[h] ?? '').length
            const headerLen  = String(h).length
            const width = Math.max(headerLen, contentLen) + 2
            return { wch: Math.min(Math.max(width, 10), 40) }
        })

        const wb = xlsx.utils.book_new()
        xlsx.utils.book_append_sheet(wb, ws, 'Record')
        xlsx.writeFile(wb, `${buildFileBase(row)}.xlsx`)
        ElMessage.success('单条 Excel 导出完成')
    } catch (e) {
        ElMessage.error('未检测到 xlsx 依赖，请先安装：npm i xlsx')
        throw e
    }
}
