package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.dict.entity.SysDictType;
import me.north30.erp.system.dict.mapper.SysDictTypeMapper;
import me.north30.erp.system.dict.service.SysDictTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 字典类型基础服务实现（只读守卫，管理端编排见 {@link DictManagementServiceImpl}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl implements SysDictTypeService {

    private final SysDictTypeMapper sysDictTypeMapper;

    @Override
    @Transactional(readOnly = true)
    public SysDictType requireByDictType(String dictType) {
        SysDictType dictTypeEntity = sysDictTypeMapper.selectOne(
            new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType));
        if (dictTypeEntity == null) {
            throw new BusinessException(SystemManageErrorCode.DICT_TYPE_NOT_FOUND, "字典类型 " + dictType + " 不存在");
        }
        return dictTypeEntity;
    }
}
