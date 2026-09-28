package me.north30.erp.system.dept;

import me.north30.erp.system.dept.dto.DeptCreateDTO;
import me.north30.erp.system.dept.dto.DeptTreeQueryDTO;
import me.north30.erp.system.dept.dto.DeptUpdateDTO;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.vo.DeptTreeVO;

/**
 * 组织域测试数据静态工厂：集中构造实体与 DTO，避免测试方法内堆砌字段。
 */
public final class DeptTestFactory {

    private DeptTestFactory() {
    }

    /**
     * 构建组织实体（默认启用，负责人/电话为空）。
     */
    public static SysDept dept(Long id, String deptCode, String deptName, Long parentId, Integer deptType,
                               Integer deptLevel, String ancestors, Integer deptSort) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setDeptCode(deptCode);
        dept.setDeptName(deptName);
        dept.setParentId(parentId);
        dept.setDeptType(deptType);
        dept.setDeptLevel(deptLevel);
        dept.setAncestors(ancestors);
        dept.setDeptSort(deptSort);
        dept.setStatus(1);
        return dept;
    }

    /**
     * 顶级组织实体（parentId=0，层级 1，祖级路径 "0"）。
     */
    public static SysDept rootDept(Long id, String deptCode, String deptName, Integer deptType, Integer deptSort) {
        return dept(id, deptCode, deptName, 0L, deptType, 1, "0", deptSort);
    }

    /**
     * 新增组织请求 DTO（负责人/电话/备注固定为 null）。
     */
    public static DeptCreateDTO createDTO(String deptCode, String deptName, Long parentId, Integer deptType,
                                          Integer deptSort, Integer status) {
        return new DeptCreateDTO(deptCode, deptName, parentId, deptType, null, null, deptSort, status, null);
    }

    /**
     * 修改组织请求 DTO（负责人/电话/排序/状态/备注固定为 null 表示不修改）。
     */
    public static DeptUpdateDTO updateDTO(Long parentId, String deptName, Integer deptType, Integer version) {
        return new DeptUpdateDTO(deptName, parentId, deptType, null, null, null, null, null, version);
    }

    /**
     * 组织树查询参数（字段可按需 set）。
     */
    public static DeptTreeQueryDTO treeQueryDTO() {
        return new DeptTreeQueryDTO();
    }

    /**
     * 组织树节点 VO（DeptTreeUtil 测试用）。
     */
    public static DeptTreeVO treeVO(Long id, Long parentId, Integer deptSort) {
        DeptTreeVO vo = new DeptTreeVO();
        vo.setId(id);
        vo.setDeptCode("ORG" + id);
        vo.setDeptName("组织" + id);
        vo.setParentId(parentId);
        vo.setDeptType(1);
        vo.setDeptLevel(1);
        vo.setAncestors("0");
        vo.setDeptSort(deptSort);
        vo.setStatus(1);
        return vo;
    }
}
