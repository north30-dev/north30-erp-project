package me.north30.erp.system.core.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.core.dto.CodeSequenceQueryDTO;
import me.north30.erp.system.core.dto.CodeSequenceResetDTO;
import me.north30.erp.system.core.vo.CodeSequenceResetVO;
import me.north30.erp.system.core.vo.CodeSequenceVO;

/**
 * 单据编号序列服务接口（接口文档 5.7，45-46 号接口）。
 * <p>仅提供查询与重置，发号逻辑由后续业务模块实现。</p>
 */
public interface SysCodeSequenceService {

    /**
     * 编号序列分页查询，携带下一编号预览。
     */
    PageResult<CodeSequenceVO> page(CodeSequenceQueryDTO query);

    /**
     * 重置编号流水：仅允许重置为不小于当前已用流水号的值；目标值相同时幂等跳过。
     */
    CodeSequenceResetVO reset(CodeSequenceResetDTO dto);
}
