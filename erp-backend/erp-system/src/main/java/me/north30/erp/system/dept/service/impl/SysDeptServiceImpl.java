package me.north30.erp.system.dept.service.impl;

import lombok.RequiredArgsConstructor;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.dept.service.SysDeptService;
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
