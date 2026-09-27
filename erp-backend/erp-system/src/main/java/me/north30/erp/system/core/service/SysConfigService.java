package me.north30.erp.system.core.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.core.dto.ConfigCreateDTO;
import me.north30.erp.system.core.dto.ConfigQueryDTO;
import me.north30.erp.system.core.dto.ConfigUpdateDTO;
import me.north30.erp.system.core.vo.ConfigUpdateVO;
import me.north30.erp.system.core.vo.ConfigVO;
import me.north30.erp.system.core.vo.MutationVO;

/**
 * 系统参数管理服务接口（接口文档 5.6，41-44 号接口）。
 */
public interface SysConfigService {

    /**
     * 系统参数分页查询。
     */
    PageResult<ConfigVO> page(ConfigQueryDTO query);

    /**
     * 新增系统参数：config_key 唯一，参数值须与 valueType 匹配。
     */
    MutationVO create(ConfigCreateDTO dto);

    /**
     * 修改系统参数：参数值按 valueType 校验，乐观锁控制并发。
     */
    ConfigUpdateVO update(Long id, ConfigUpdateDTO dto);

    /**
     * 删除系统参数（逻辑删除）：内置参数（is_system=1）不可删。
     */
    MutationVO delete(Long id);
}
