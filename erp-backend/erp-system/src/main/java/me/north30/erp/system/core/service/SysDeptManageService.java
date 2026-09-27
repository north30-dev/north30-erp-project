package me.north30.erp.system.core.service;

import me.north30.erp.system.core.dto.DeptCreateDTO;
import me.north30.erp.system.core.dto.DeptTreeQueryDTO;
import me.north30.erp.system.core.dto.DeptUpdateDTO;
import me.north30.erp.system.core.vo.DeptMutationVO;
import me.north30.erp.system.core.vo.DeptTreeVO;

import java.util.List;

/**
 * 组织/部门管理服务接口（接口文档 5.4，29-32 号接口）。
 */
public interface SysDeptManageService {

    /**
     * 组织部门树查询：一次查全量后内存组树（禁 N+1）。
     */
    List<DeptTreeVO> listTree(DeptTreeQueryDTO query);

    /**
     * 新增组织部门：校验父级存在、组织编码唯一、层级 ≤5，ancestors/deptLevel 服务端计算。
     */
    DeptMutationVO create(DeptCreateDTO dto);

    /**
     * 修改组织部门：parentId 不得指向自身或自身后代（环检测），
     * 变更父级时级联重算下级 ancestors/dept_level；乐观锁控制并发。
     */
    DeptMutationVO update(Long id, DeptUpdateDTO dto);

    /**
     * 删除组织部门（逻辑删除）：有下级组织或绑定用户不可删。
     */
    DeptMutationVO delete(Long id);
}
