package me.north30.erp.system.config.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.config.dto.ConfigCreateDTO;
import me.north30.erp.system.config.dto.ConfigQueryDTO;
import me.north30.erp.system.config.dto.ConfigStatusDTO;
import me.north30.erp.system.config.dto.ConfigUpdateDTO;
import me.north30.erp.system.config.vo.ConfigStatusVO;
import me.north30.erp.system.config.vo.ConfigUpdateVO;
import me.north30.erp.system.config.vo.ConfigVO;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.common.vo.MutationVO;

/**
 * 系统参数管理应用服务（管理端用例编排，接口文档 5.6，41-44 号接口）。
 * <p>跨域只读取数不走本服务，统一走 {@link SysConfigDomainService}。</p>
 */
public interface SysConfigApplicationService {

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
    DeleteResultVO delete(Long id);

    /**
     * 变更系统参数状态（启用/停用，08 文档阶段四状态开关统一）：内置参数（is_system=1）不可停用。
     */
    ConfigStatusVO changeStatus(Long id, ConfigStatusDTO dto);
}
