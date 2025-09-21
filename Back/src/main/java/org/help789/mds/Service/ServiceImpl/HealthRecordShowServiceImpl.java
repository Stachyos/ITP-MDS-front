package org.help789.mds.Service.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.help789.mds.Service.HealthRecordShowService;
import org.help789.mds.Entity.Vo.HealthRecordShowVo;
import org.help789.mds.Entity.HealthRecord;
import org.help789.mds.repository.HealthRecordRepository;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthRecordShowServiceImpl implements HealthRecordShowService {

    private final HealthRecordRepository repository;

    // ================== 查询 ==================
    @Override
    public List<HealthRecordShowVo> listAll() {
        return repository.findAll()
                .stream()
                .map(this::entity2Vo)
                .collect(Collectors.toList());
    }

    // ================== 新增 ==================
    @Override
    public HealthRecordShowVo create(HealthRecordShowVo vo) {
        HealthRecord entity = vo2Entity(vo);
        entity.setRecordId(null); // 由数据库自增
        HealthRecord saved = repository.save(entity);
        return entity2Vo(saved);
    }

    // ================== 修改 ==================
    @Override
    public HealthRecordShowVo update(Long recordId, HealthRecordShowVo vo) {
        HealthRecord exist = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        HealthRecord patch = vo2Entity(vo);
        patch.setRecordId(exist.getRecordId()); // 保留ID
        HealthRecord saved = repository.save(patch);
        return entity2Vo(saved);
    }

    // ================== 删除 ==================
    @Override
    public void delete(Long recordId) {
        if (!repository.existsById(recordId)) {
            throw new IllegalArgumentException("Record not found: " + recordId);
        }
        repository.deleteById(recordId);
    }

    // ================== 映射：Entity -> VO ==================
    private HealthRecordShowVo entity2Vo(HealthRecord entity) {
        HealthRecordShowVo vo = new HealthRecordShowVo();
        vo.setRecordId(entity.getRecordId());
        vo.setName(entity.getName());

        // sex: 1=男，其余=女（含null）
        vo.setSex(sexIntToStr(entity.getSex()));

        vo.setAge(entity.getAge());
        vo.setHdlC(entity.getHdlC());
        vo.setLdlC(entity.getLdlC());
        vo.setVldlC(entity.getVldlC());
        vo.setTriglyceride(entity.getTriglyceride());
        vo.setTotalCholesterol(entity.getTotalCholesterol());
        vo.setPulse(entity.getPulse());
        vo.setDiastolicBp(entity.getDiastolicBp());
        vo.setHypertensionHistory(entity.getHypertensionHistory());
        vo.setBun(entity.getBun());
        vo.setUricAcid(entity.getUricAcid());
        vo.setCreatinine(entity.getCreatinine());
        return vo;
    }

    // ================== 映射：VO -> Entity ==================
    private HealthRecord vo2Entity(HealthRecordShowVo vo) {
        HealthRecord entity = new HealthRecord();
        entity.setRecordId(vo.getRecordId());
        entity.setName(vo.getName());

        // sex: 支持 "男/女"、"1/0"、"male/female"、"m/f"（默认 0=女）
        entity.setSex(parseSexToInt(vo.getSex()));

        entity.setAge(vo.getAge());
        entity.setHdlC(vo.getHdlC());
        entity.setLdlC(vo.getLdlC());
        entity.setVldlC(vo.getVldlC());
        entity.setTriglyceride(vo.getTriglyceride());
        entity.setTotalCholesterol(vo.getTotalCholesterol());
        entity.setPulse(vo.getPulse());
        entity.setDiastolicBp(vo.getDiastolicBp());
        entity.setHypertensionHistory(Boolean.TRUE.equals(vo.getHypertensionHistory()));
        entity.setBun(vo.getBun());
        entity.setUricAcid(vo.getUricAcid());
        entity.setCreatinine(vo.getCreatinine());
        return entity;
    }

    // ================== 性别映射辅助 ==================

    /** Entity: 1=男，其余=女 */
    private String sexIntToStr(Integer sex) {
        return (sex != null && sex == 1) ? "男" : "女";
    }

    /** VO -> Entity: "男/1/male/m" = 1；其他=0 */
    private Integer parseSexToInt(String sexStr) {
        if (sexStr == null) return 0;
        String s = sexStr.trim().toLowerCase();
        if ("1".equals(s) || "男".equals(sexStr) || "male".equals(s) || "m".equals(s)) return 1;
        // 其余（"0"、"女"、"female"、"f"、"其他"、"未知"等）都按 0 处理
        return 0;
    }

    private static final String[] HEADERS = {
            "卡号","性别","年龄",
            "高密度脂蛋白胆固醇","低密度脂蛋白胆固醇","极低密度脂蛋白胆固醇",
            "甘油三酯","总胆固醇","脉搏","舒张压","高血压史",
            "尿素氮","尿酸","肌酐","体重检查结果","是否糖尿病"
    };

    @Override
    public void writeTemplate(OutputStream out) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("健康数据模板");

            // 表头样式：加粗、居中、浅灰底
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // 表头
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                // 适配列宽（中文大概 2 倍宽度），并设上限避免过大
                int width = Math.min(10000, (HEADERS[i].length() + 4) * 512);
                sheet.setColumnWidth(i, width);
            }

            // 冻结表头
            sheet.createFreezePane(0, 1);

            // 可选：放一行提示（如果不需要示例，删除下面 5 行）
            Row hint = sheet.createRow(1);
            hint.createCell(0).setCellValue("示例/说明：");
            hint.createCell(1).setCellValue("性别 0=女,1=男");
            hint.createCell(10).setCellValue("高血压史 0=否,1=是");
            hint.createCell(15).setCellValue("是否糖尿病 0=否,1=是");

            wb.write(out);
        } catch (Exception e) {
            // 这里不要吞异常，上层会写 response；记录日志便于排查
            throw new RuntimeException("生成健康数据模板失败", e);
        }
    }
}
