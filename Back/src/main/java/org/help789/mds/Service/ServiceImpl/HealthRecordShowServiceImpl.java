package org.help789.mds.Service.ServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.help789.mds.Service.HealthRecordShowService;
import org.help789.mds.Entity.Vo.HealthRecordShowVo;
import org.help789.mds.Entity.HealthRecord;
import org.help789.mds.Utils.pojo.ImportResult;
import org.help789.mds.repository.HealthRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    private static final Set<String> NAME_KEYS = setOf("卡号","姓名","名称","名称/标签");

    /** 体重检查结果允许值（可按需扩展） */
    private static final Set<String> ALLOWED_WEIGHT_RESULTS =
            new LinkedHashSet<>(Arrays.asList("正常","偏瘦","偏胖","超重","肥胖","未知"));

    private static final Map<String,String> HEADER_MAP = buildHeaderMap();

    @Override
    public ImportResult importData(MultipartFile file, String format) {
        String fmt = resolveFormat(file, format);  // csv|excel|json
        List<Map<String, String>> rows;           // 先统一成「列名->字符串」

        try {
            switch (fmt) {
                case "csv"   -> rows = parseCsv(file);
                case "excel" -> rows = parseExcel(file);
                case "json"  -> rows = parseJson(file);
                default      -> throw new IllegalArgumentException("不支持的导入格式：" + fmt);
            }
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败：" + e.getMessage(), e);
        }

        int total = rows.size();
        int skippedKey = 0;
        int skippedAbnormal = 0;

        // —— 预处理：行 -> 实体（缺失值填充 + 异常过滤）——
        List<HealthRecord> prepared = new ArrayList<>();
        for (Map<String, String> raw : rows) {
            Optional<HealthRecord> maybe = toEntity(raw);
            if (maybe.isEmpty()) {
                // 关键字段缺失或异常
                if (isMissingKey(raw)) skippedKey++;
                else skippedAbnormal++;
                continue;
            }
            prepared.add(maybe.get());
        }

        // —— 去重：优先 recordId，其次 name；保留最后出现的 ——（LinkedHashMap 实现“后覆盖”）
        int beforeDedup = prepared.size();
        Map<String, HealthRecord> dedup = new LinkedHashMap<>();
        for (HealthRecord hr : prepared) {
            String key = dedupKey(hr);
            if (key == null) continue; // 理论不会发生，因为 name 已校验
            dedup.put(key, hr); // 覆盖旧值 => 保留最新
        }
        int deduped = beforeDedup - dedup.size();

        // —— 入库 ——（批量保存）
        List<HealthRecord> toSave = new ArrayList<>(dedup.values());
        repository.saveAll(toSave);

        return ImportResult.builder()
                .totalRows(total)
                .skippedMissingKey(skippedKey)
                .skippedAbnormal(skippedAbnormal)
                .deduplicated(deduped)
                .saved(toSave.size())
                .message("导入完成")
                .build();
    }

    /* ============================ 解析器 ============================ */

    private List<Map<String, String>> parseCsv(MultipartFile file) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()     // 第一行做表头
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {
            List<Map<String,String>> list = new ArrayList<>();
            for (CSVRecord r : parser) {
                Map<String,String> row = new LinkedHashMap<>();
                for (String h : parser.getHeaderNames()) {
                    row.put(normalizeHeader(h), r.get(h));
                }
                list.add(row);
            }
            return list;
        }
    }

    private List<Map<String, String>> parseExcel(MultipartFile file) throws IOException {
        try (Workbook wb = file.getOriginalFilename()!=null && file.getOriginalFilename().toLowerCase().endsWith(".xls")
                ? WorkbookFactory.create(file.getInputStream()) // 兼容xls
                : new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) return List.of();
            // 表头
            Row headerRow = it.next();
            List<String> headers = new ArrayList<>();
            for (Cell c : headerRow) headers.add(normalizeHeader(getCellString(c)));

            // 数据
            List<Map<String,String>> list = new ArrayList<>();
            while (it.hasNext()) {
                Row r = it.next();
                Map<String,String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell c = r.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    row.put(headers.get(i), getCellString(c));
                }
                list.add(row);
            }
            return list;
        }
    }

    private List<Map<String, String>> parseJson(MultipartFile file) throws IOException {
        ObjectMapper m = new ObjectMapper();
        m.findAndRegisterModules();
        // 支持两种结构：[{...},{...}] 或 {"rows":[{...}]}
        JsonNode root = m.readTree(file.getInputStream());
        List<Map<String,String>> list = new ArrayList<>();
        if (root.isArray()) {
            for (JsonNode n : root) list.add(nodeToMap(n));
        } else if (root.isObject() && root.has("rows") && root.get("rows").isArray()) {
            for (JsonNode n : root.get("rows")) list.add(nodeToMap(n));
        } else {
            throw new IllegalArgumentException("JSON 结构需为数组或含 rows 的对象");
        }
        return list;
    }

    private Map<String,String> nodeToMap(JsonNode n) {
        Map<String,String> m = new LinkedHashMap<>();
        n.fieldNames().forEachRemaining(fn -> {
            String key = normalizeHeader(fn);
            String val = n.get(fn).isNull() ? "" : n.get(fn).asText();
            m.put(key, val);
        });
        return m;
    }

    /* ============================ 行 -> 实体 ============================ */

    private Optional<HealthRecord> toEntity(Map<String,String> row) {
        String name = firstNonBlank(row, NAME_KEYS);
        if (isBlank(name)) return Optional.empty(); // 仅此一种会丢行

        Integer sex = parseSex(row.get(normalizeHeader("性别")));
        Boolean htn = parseBool(row.get(normalizeHeader("高血压史")));
        String  weightRes = parseWeightResult(row.get(normalizeHeader("体重检查结果")));
        Boolean diabetes = parseBool(row.get(normalizeHeader("是否糖尿病")));

        java.math.BigDecimal hdl  = parseDecimalOrZero(row.get(normalizeHeader("高密度脂蛋白胆固醇")));
        java.math.BigDecimal ldl  = parseDecimalOrZero(row.get(normalizeHeader("低密度脂蛋白胆固醇")));
        java.math.BigDecimal vldl = parseDecimalOrZero(row.get(normalizeHeader("极低密度脂蛋白胆固醇")));
        java.math.BigDecimal tg   = parseDecimalOrZero(row.get(normalizeHeader("甘油三酯")));
        java.math.BigDecimal tc   = parseDecimalOrZero(row.get(normalizeHeader("总胆固醇")));
        java.math.BigDecimal bun  = parseDecimalOrZero(row.get(normalizeHeader("尿素氮")));
        java.math.BigDecimal ua   = parseDecimalOrZero(row.get(normalizeHeader("尿酸")));
        java.math.BigDecimal crt  = parseDecimalOrZero(row.get(normalizeHeader("肌酐")));

        Integer age   = parseIntOrZero(row.get(normalizeHeader("年龄")));
        Integer pulse = parseIntOrZero(row.get(normalizeHeader("脉搏")));
        Integer dbp   = parseIntOrZero(row.get(normalizeHeader("舒张压")));

        HealthRecord hr = HealthRecord.builder()
                .recordId(parseLongNullable(row.get(normalizeHeader("recordId"))))
                .name(name.trim())
                .sex(sex)
                .age(age)
                .hdlC(hdl).ldlC(ldl).vldlC(vldl)
                .triglyceride(tg).totalCholesterol(tc)
                .pulse(pulse).diastolicBp(dbp)
                .hypertensionHistory(htn)
                .bun(bun).uricAcid(ua).creatinine(crt)
                .weightCheckResult(weightRes)
                .diabetes(diabetes)
                .build();
        return Optional.of(hr);
    }

    /* ============================ 工具 & 校验 ============================ */

    private static String resolveFormat(MultipartFile f, String fmt) {
        if (fmt != null && !fmt.isBlank()) return fmt.toLowerCase();
        String name = Optional.ofNullable(f.getOriginalFilename()).orElse("").toLowerCase();
        if (name.endsWith(".csv"))  return "csv";
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) return "excel";
        if (name.endsWith(".json")) return "json";
        throw new IllegalArgumentException("无法从文件名识别格式，请显式指定 ?format=csv|excel|json");
    }

    private static String getCellString(Cell c) {
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue().trim();
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(c)) yield String.valueOf(c.getNumericCellValue());
                double v = c.getNumericCellValue();
                // 去掉类似 12.0 的小尾巴
                String s = BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
                yield s;
            }
            case FORMULA -> c.getCellFormula();
            default -> "";
        };
    }

    private static String normalizeHeader(String h) {
        return Optional.ofNullable(h).orElse("").trim().toLowerCase();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String firstNonBlank(Map<String,String> row, Set<String> candidates) {
        for (String cn : candidates) {
            String v = row.get(normalizeHeader(cn));
            if (!isBlank(v)) return v;
        }
        return null;
    }

    private static Set<String> setOf(String... a) {
        return new LinkedHashSet<>(Arrays.asList(a));
    }

    private static Map<String,String> buildHeaderMap() {
        Map<String,String> m = new HashMap<>();
        // 可在需要时做“中文->内部字段”的映射表，这里主要用于存在时的读取；当前直接按 normalize 后的中文键读取
        return m;
    }

    private static boolean isMissingKey(Map<String,String> row) {
        return isBlank(firstNonBlank(row, NAME_KEYS));
    }

    // 允许：0/1/男/女/male/female/m/f/未知；空缺→0（女）；未知→0
    private static Integer parseSex(String s) {
        if (isBlank(s)) return 0;
        String t = s.trim().toLowerCase();
        if (List.of("1","男","male","m","男1","1.0").contains(t)) return 1;
        if (List.of("0","女","female","f","女0","0.0","未知").contains(t)) return 0;
        // 其余视为未知→0
        return 0;
    }

    // 允许：1/0/是/否/true/false/y/n/有/无/阳性/阴性/√/×；空缺→false
    private static Boolean parseBool(String s) {
        if (isBlank(s)) return false;
        String t = s.trim().toLowerCase();
        if (List.of("1","是","true","y","yes","有","阳性","√").contains(t)) return true;
        if (List.of("0","否","false","n","no","无","阴性","×").contains(t)) return false;
        // 不认识的统一按 false
        return false;
    }

    // 体重检查结果：常见写法标准化；空缺或不识别→"未知"
    private static String parseWeightResult(String s) {
        if (isBlank(s)) return "未知";
        String v = s.trim().toLowerCase();
        if (List.of("正常","正常体重").contains(v)) return "正常";
        if (List.of("偏瘦","过轻").contains(v)) return "偏瘦";
        if (List.of("偏胖").contains(v)) return "偏胖";
        if (List.of("超重","过重").contains(v)) return "超重";
        if (v.startsWith("肥胖")) return "肥胖"; // 肥胖i/ii/iii度
        if (List.of("未知","--","-","—","/").contains(v)) return "未知";
        return "未知";
    }

    // 数值：允许带单位/逗号/空格，用正则提取第一个数字；
// 常见占位(-、—、/、无、未检、未测、n/a、null)或提取失败 → 0
    private static java.math.BigDecimal parseDecimalOrZero(String s) {
        if (isBlank(s)) return java.math.BigDecimal.ZERO;
        String t = s.trim().toLowerCase()
                .replace(",", "")
                .replace("mmol/l", "")
                .replace("mg/dl", "")
                .replace("次/分", "")
                .replace("mmhg", "")
                .replace(" ", "");
        if (List.of("-", "—", "--", "/", "无", "未检", "未测", "n/a", "na", "null").contains(t))
            return java.math.BigDecimal.ZERO;

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("[-+]?\\d+(?:\\.\\d+)?")
                .matcher(t);
        if (m.find()) return new java.math.BigDecimal(m.group());
        return java.math.BigDecimal.ZERO;
    }

    // 整数：逻辑同上
    private static Integer parseIntOrZero(String s) {
        if (isBlank(s)) return 0;
        String t = s.trim().toLowerCase().replace(",", "").replace(" ", "");
        if (List.of("-", "—", "--", "/", "无", "未检", "未测", "n/a", "na", "null").contains(t))
            return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("[-+]?\\d+")
                .matcher(t);
        if (m.find()) return Integer.parseInt(m.group());
        return 0;
    }

    private static Long parseLongNullable(String s) {
        if (isBlank(s)) return null;
        try { return Long.valueOf(s.trim()); } catch (Exception e) { return null; }
    }

    /** 去重 key：优先 recordId，其次 name */
    private static String dedupKey(HealthRecord hr) {
        if (hr.getRecordId() != null) return "ID:" + hr.getRecordId();
        if (!isBlank(hr.getName())) return "NAME:" + hr.getName().trim();
        return null;
    }
}
