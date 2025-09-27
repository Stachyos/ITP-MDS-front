// src/utils/health/assess.js
function sexOf(row) {
    const s = String(row?.sex ?? '').toLowerCase()
    if (['女', 'female', 'f', 'woman'].some(x => s.includes(x))) return 'female'
    if (['男', 'male', 'm', 'man'].some(x => s.includes(x))) return 'male'
    return 'unknown'
}

function labelStatus(code, note = '') {
    const map = {
        normal:     { text: `Normal${note ? ` (${note})` : ''}`,     color: [34, 139, 34] },
        borderline: { text: `Borderline${note ? ` (${note})` : ''}`, color: [218, 140, 0] },
        high:       { text: `High${note ? ` (${note})` : ''}`,       color: [200, 0, 0] },
        low:        { text: `Low${note ? ` (${note})` : ''}`,        color: [200, 0, 0] },
        condition:  { text: `Condition${note ? ` (${note})` : ''}`,  color: [200, 0, 0] },
        unknown:    { text: 'Unknown',                                color: [120, 120, 120] },
    }
    return map[code] || map.unknown
}

function asNumber(v) {
    const n = Number(v)
    return Number.isFinite(n) ? n : null
}

/** 返回：[{ item, valueStr, statusText, statusColor }] */
export function assessMetrics(row) {
    const sex = sexOf(row)
    const out = []
    const push = (item, v, status) => {
        const valStr = (v ?? v === 0) ? String(v) : '-'
        out.push({ item, valueStr: valStr, statusText: status.text, statusColor: status.color })
    }

    // Total Cholesterol
    {
        const v = asNumber(row.totalCholesterol)
        if (v == null) push('Total Cholesterol', v, labelStatus('unknown'))
        else if (v < 200) push('Total Cholesterol', v, labelStatus('normal', '<200'))
        else if (v < 240) push('Total Cholesterol', v, labelStatus('borderline', '200-239'))
        else push('Total Cholesterol', v, labelStatus('high', '>=240'))
    }

    // Triglycerides
    {
        const v = asNumber(row.triglyceride)
        if (v == null) push('Triglycerides', v, labelStatus('unknown'))
        else if (v < 150) push('Triglycerides', v, labelStatus('normal', '<150'))
        else if (v < 200) push('Triglycerides', v, labelStatus('borderline', '150-199'))
        else if (v < 500) push('Triglycerides', v, labelStatus('high', '200-499'))
        else push('Triglycerides', v, labelStatus('high', '>=500'))
    }

    // HDL-C
    {
        const v = asNumber(row.hdlC)
        if (v == null) push('HDL-C', v, labelStatus('unknown'))
        else {
            const lowCut = sex === 'female' ? 50 : 40
            if (v >= 60) push('HDL-C', v, { text: 'Optimal (>=60)', color: [34, 139, 34] })
            else if (v >= lowCut) push('HDL-C', v, labelStatus('normal', `>=${lowCut}`))
            else push('HDL-C', v, labelStatus('low', `<${lowCut}`))
        }
    }

    // LDL-C
    {
        const v = asNumber(row.ldlC)
        if (v == null) push('LDL-C', v, labelStatus('unknown'))
        else if (v < 100) push('LDL-C', v, { text: 'Optimal (<100)', color: [34, 139, 34] })
        else if (v < 130) push('LDL-C', v, labelStatus('normal', '100-129'))
        else if (v < 160) push('LDL-C', v, labelStatus('borderline', '130-159'))
        else if (v < 190) push('LDL-C', v, labelStatus('high', '160-189'))
        else push('LDL-C', v, labelStatus('high', '>=190'))
    }

    // VLDL-C (2-30)
    {
        const v = asNumber(row.vldlC)
        if (v == null) push('VLDL-C', v, labelStatus('unknown'))
        else if (v <= 30 && v >= 2) push('VLDL-C', v, labelStatus('normal', '2-30'))
        else if (v > 30) push('VLDL-C', v, labelStatus('high', '>30'))
        else push('VLDL-C', v, labelStatus('low', '<2'))
    }

    // Pulse
    {
        const v = asNumber(row.pulse)
        if (v == null) push('Pulse (bpm)', v, labelStatus('unknown'))
        else if (v >= 60 && v <= 100) push('Pulse (bpm)', v, labelStatus('normal', '60-100'))
        else if (v < 60) push('Pulse (bpm)', v, labelStatus('low', '<60'))
        else push('Pulse (bpm)', v, labelStatus('high', '>100'))
    }

    // Diastolic BP
    {
        const v = asNumber(row.diastolicBp)
        if (v == null) push('Diastolic BP (mmHg)', v, labelStatus('unknown'))
        else if (v < 80) push('Diastolic BP (mmHg)', v, labelStatus('normal', '<80'))
        else if (v < 90) push('Diastolic BP (mmHg)', v, labelStatus('borderline', '80-89'))
        else push('Diastolic BP (mmHg)', v, labelStatus('high', '>=90'))
    }

    // Hypertension history
    {
        const v = !!row.hypertensionHistory
        if (v) push('Hypertension History', 'Yes', labelStatus('condition'))
        else push('Hypertension History', 'No', labelStatus('normal'))
    }

    // BUN
    {
        const v = asNumber(row.bun)
        if (v == null) push('BUN', v, labelStatus('unknown'))
        else if (v >= 7 && v <= 20) push('BUN', v, labelStatus('normal', '7-20'))
        else if (v < 7) push('BUN', v, labelStatus('low', '<7'))
        else push('BUN', v, labelStatus('high', '>20'))
    }

    // Uric Acid (sex-specific)
    {
        const v = asNumber(row.uricAcid)
        if (v == null) push('Uric Acid', v, labelStatus('unknown'))
        else if (sex === 'female') {
            if (v >= 2.4 && v <= 6.0) push('Uric Acid', v, labelStatus('normal', '2.4-6.0 (F)'))
            else if (v < 2.4) push('Uric Acid', v, labelStatus('low', '<2.4 (F)'))
            else push('Uric Acid', v, labelStatus('high', '>6.0 (F)'))
        } else {
            if (v >= 3.4 && v <= 7.0) push('Uric Acid', v, labelStatus('normal', '3.4-7.0 (M)'))
            else if (v < 3.4) push('Uric Acid', v, labelStatus('low', '<3.4 (M)'))
            else push('Uric Acid', v, labelStatus('high', '>7.0 (M)'))
        }
    }

    // Creatinine (sex-specific)
    {
        const v = asNumber(row.creatinine)
        if (v == null) push('Creatinine', v, labelStatus('unknown'))
        else if (sex === 'female') {
            if (v >= 0.59 && v <= 1.04) push('Creatinine', v, labelStatus('normal', '0.59-1.04 (F)'))
            else if (v < 0.59) push('Creatinine', v, labelStatus('low', '<0.59 (F)'))
            else push('Creatinine', v, labelStatus('high', '>1.04 (F)'))
        } else {
            if (v >= 0.74 && v <= 1.35) push('Creatinine', v, labelStatus('normal', '0.74-1.35 (M)'))
            else if (v < 0.74) push('Creatinine', v, labelStatus('low', '<0.74 (M)'))
            else push('Creatinine', v, labelStatus('high', '>1.35 (M)'))
        }
    }

    return out
}
