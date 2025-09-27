import { buildOneRow, buildFileBase, ts } from './common.js'
import { ElMessage } from 'element-plus'

export const exportRecordPDF = async (row) => {
    try {
        const jsPDF = (await import('jspdf')).default
        let autoTable
        try { autoTable = (await import('jspdf-autotable')).default } catch {}

        const doc = new jsPDF({ orientation: 'portrait', unit: 'pt', format: 'a4' })
        const margin = { left: 56, right: 56, top: 68, bottom: 56 }
        const pageWidth = doc.internal.pageSize.getWidth()

        // 标题
        doc.setFontSize(18)
        doc.text('健康数据个人报告 / Personal Health Record', margin.left, margin.top)

        // 分割线
        const lineY = margin.top + 14
        doc.setDrawColor(230)
        doc.line(margin.left, lineY, pageWidth - margin.right, lineY)

        // 信息卡
        const infoY = lineY + 18
        doc.setFontSize(12)
        const safe = (v) => (v ?? '-') + ''
        const leftX  = margin.left
        const rightX = pageWidth / 2
        doc.text(`姓名 / Name：${safe(row.name)}`, leftX,  infoY)
        doc.text(`性别 / Gender：${safe(row.sex)}`, leftX,  infoY + 18)
        doc.text(`年龄 / Age：${safe(row.age)}`,   leftX,  infoY + 36)
        doc.text(`记录 ID：${safe(row.recordId)}`, rightX, infoY)
        doc.text(`导出时间：${new Date().toLocaleString()}`, rightX, infoY + 18)

        // 表格标题
        let y = infoY + 56
        doc.setFontSize(14)
        doc.text('关键指标 / Key Metrics', margin.left, y)
        y += 8
        doc.setDrawColor(240); doc.line(margin.left, y, pageWidth - margin.right, y)
        y += 10

        // 表格数据
        const r = buildOneRow(row)
        const headers = ['项目 / Item', '数值 / Value']
        const body = Object.entries(r).map(([k, v]) => [k, String(v ?? '')])

        if (autoTable) {
            autoTable(doc, {
                head: [headers],
                body,
                startY: y,
                margin,
                styles: {
                    fontSize: 11,
                    cellPadding: 6,
                    overflow: 'linebreak',
                    lineColor: 230,
                    lineWidth: 0.5,
                },
                headStyles: {
                    fillColor: [64, 158, 255],
                    textColor: 255,
                    fontStyle: 'bold',
                },
                alternateRowStyles: { fillColor: [248, 250, 252] },
                columnStyles: { 0: { cellWidth: 220 }, 1: { cellWidth: 'auto' } },
                didDrawPage: () => {
                    const page = doc.internal.getCurrentPageInfo().pageNumber
                    const total = doc.getNumberOfPages()
                    doc.setFontSize(10); doc.setTextColor(130)
                    doc.text(
                        `第 ${page} / ${total} 页`,
                        pageWidth - margin.right,
                        doc.internal.pageSize.getHeight() - 26,
                        { align: 'right' }
                    )
                },
            })
        } else {
            // 无 autotable 的兜底
            doc.setFontSize(11)
            let yy = y
            doc.text(headers.join(' | '), margin.left, yy); yy += 16
            body.forEach((row2) => {
                doc.text(row2.join(' : '), margin.left, yy)
                yy += 14
                if (yy > doc.internal.pageSize.getHeight() - margin.bottom) {
                    doc.addPage(); yy = margin.top
                }
            })
        }

        doc.save(`${buildFileBase(row)}.pdf`)
        ElMessage.success('单条 PDF 报告已导出')
    } catch (e) {
        ElMessage.error('PDF 导出失败，如需表格排版请安装：npm i jspdf jspdf-autotable')
        throw e
    }
}
