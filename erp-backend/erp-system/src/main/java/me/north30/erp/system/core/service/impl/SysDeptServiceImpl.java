package me.north30.erp.system.core.service.impl;

import lombok.RequiredArgsConstructor;
import me.north30.erp.system.core.entity.SysDept;
import me.north30.erp.system.core.mapper.SysDeptMapper;
import me.north30.erp.system.core.service.SysDeptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 组织/部门服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper sysDeptMapper;

    @Override
    @Transactional(readOnly = true)
    public SysDept getById(Long id) {
        return sysDeptMapper.selectById(id);
    }
}
