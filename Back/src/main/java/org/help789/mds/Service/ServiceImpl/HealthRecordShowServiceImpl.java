package org.help789.mds.Service.ServiceImpl;

import lombok.RequiredArgsConstructor;
import org.help789.mds.Service.HealthRecordShowService;
import org.help789.mds.Entity.Vo.HealthRecordShowVo;
import org.help789.mds.Entity.HealthRecord;
import org.help789.mds.repository.HealthRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
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
        // 由数据库自增
        entity.setRecordId(null);
        HealthRecord saved = repository.save(entity);
        return entity2Vo(saved);
    }

    // ================== 修改 ==================
    @Override
    public HealthRecordShowVo update(Long recordId, HealthRecordShowVo vo) {
        HealthRecord exist = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        // 将 vo 覆盖到已存在实体上（保留ID）
        HealthRecord patch = vo2Entity(vo);
        patch.setRecordId(exist.getRecordId());
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
        vo.setSex(entity.getSex() == null ? null : entity.getSex().name());
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
        entity.setSex(parseSex(vo.getSex()));
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

    private HealthRecord.Sex parseSex(String sexStr) {
        if (sexStr == null) return HealthRecord.Sex.未知;
        String s = sexStr.trim();
        // 兼容中英文 & 大小写
        if (equalsAnyIgnoreCase(s, "男", "male", "m")) return HealthRecord.Sex.男;
        if (equalsAnyIgnoreCase(s, "女", "female", "f")) return HealthRecord.Sex.女;
        if (equalsAnyIgnoreCase(s, "其他", "other")) return HealthRecord.Sex.其他;
        if (equalsAnyIgnoreCase(s, "未知", "unknown", "unk")) return HealthRecord.Sex.未知;
        // 回退：尝试按枚举名解析；再不行就未知
        try { return HealthRecord.Sex.valueOf(s); }
        catch (Exception ignore) { return HealthRecord.Sex.未知; }
    }

    private boolean equalsAnyIgnoreCase(String a, String... arr) {
        for (String b : arr) if (Objects.equals(a.toLowerCase(), b.toLowerCase())) return true;
        return false;
    }
}
