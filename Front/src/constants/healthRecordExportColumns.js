// 列定义与映射（供导出复用）

export const EXPORT_COLUMNS = [
    { key: 'recordId',         title: 'ID' },
    { key: 'name',             title: 'Name/Label' },
    { key: 'sex',              title: 'Gender', format: mapSex },
    { key: 'age',              title: 'Age' },
    { key: 'totalCholesterol', title: 'Total Cholesterol' },
    { key: 'triglyceride',     title: 'Triglycerides' },
    { key: 'hdlC',             title: 'HDL-C' },
    { key: 'ldlC',             title: 'LDL-C' },
    { key: 'vldlC',            title: 'VLDL-C' },
    { key: 'pulse',            title: 'Pulse' },
    { key: 'diastolicBp',      title: 'Diastolic BP' },
    { key: 'hypertensionHistory', title: 'Hypertension History', format: mapYesNo },
    { key: 'bun',              title: 'Blood Urea Nitrogen (BUN)' },
    { key: 'uricAcid',         title: 'Uric Acid' },
    { key: 'creatinine',       title: 'Creatinine' },
]

// —— 映射函数
export function mapSex(v) {
    const s = String(v ?? '').trim().toLowerCase()
    if (['男','m','male','man','1'].includes(s)) return 'Male'
    if (['女','f','female','woman','0'].includes(s)) return 'Female'
    if (['其他','other'].includes(s)) return 'Other'
    if (['未知','unknown','unk',''].includes(s)) return 'Unknown'
    return v ?? ''
}

export function mapYesNo(v) {
    if (typeof v === 'boolean') return v ? 'Yes' : 'No'
    const s = String(v ?? '').trim().toLowerCase()
    if (['是','yes','y','true','1'].includes(s)) return 'Yes'
    if (['否','no','n','false','0'].includes(s)) return 'No'
    return v ?? ''
}
