package org.help789.mds.Service.ServiceImpl;

import org.help789.mds.Entity.Permission;
import org.help789.mds.Entity.User;
import org.help789.mds.repository.PermissionRepository;
import org.help789.mds.repository.UserRepository;
import org.help789.mds.Service.PermissionService;
import org.help789.mds.Entity.Vo.PermissionVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取所有权限（补充 account/email）
     */
    @Override
    public List<PermissionVo> getAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 根据用户ID获取权限（补充 account/email）
     */
    @Override
    public PermissionVo getPermissionByUserId(Long userId) {
        return permissionRepository.findByUserId(userId)
                .map(this::toVo)
                .orElse(null);
    }

    /**
     * 保存/更新权限（保存前校验 userId 是否存在于 users 表）
     */
    @Override
    public PermissionVo savePermission(PermissionVo permissionVo) {
        // 校验 userId 是否存在
        User user = userRepository.findById(permissionVo.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在，userId=" + permissionVo.getUserId()));

        // 保存 Permission
        Permission entity = toEntity(permissionVo);
        Permission saved = permissionRepository.save(entity);

        // 转换成 VO 并补充 account/email
        PermissionVo result = toVo(saved);
        result.setAccount(user.getAccount());
        result.setEmail(user.getEmail());

        return result;
    }

    /**
     * 删除权限记录
     */
    @Override
    public void deletePermission(Long permissionId) {
        permissionRepository.deleteById(permissionId);
    }

    // ========= 内部转换方法 =========

    private PermissionVo toVo(Permission entity) {
        PermissionVo vo = new PermissionVo();
        BeanUtils.copyProperties(entity, vo);

        // ⭐ 通过 userId 关联 users 表，补充 account/email
        userRepository.findById(entity.getUserId()).ifPresent(user -> {
            vo.setAccount(user.getAccount());
            vo.setEmail(user.getEmail());
        });

        return vo;
    }

    private Permission toEntity(PermissionVo vo) {
        Permission entity = new Permission();
        BeanUtils.copyProperties(vo, entity);
        return entity;
    }
}
