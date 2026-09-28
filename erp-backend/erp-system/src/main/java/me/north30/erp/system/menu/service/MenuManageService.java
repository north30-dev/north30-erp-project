package me.north30.erp.system.menu.service;

import java.util.List;

import me.north30.erp.system.menu.dto.MenuCreateDTO;
import me.north30.erp.system.menu.dto.MenuTreeQueryDTO;
import me.north30.erp.system.menu.dto.MenuUpdateDTO;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.menu.vo.MenuCreatedVO;
import me.north30.erp.system.menu.vo.MenuNodeVO;
import me.north30.erp.system.menu.vo.MenuUpdatedVO;

/**
 * 菜单与权限点管理服务接口（接口文档 5.3，4 个接口）。
 */
public interface MenuManageService {

    /**
     * 菜单树查询（一次查全部再内存组树，禁止 N+1）。
     */
    List<MenuNodeVO> tree(MenuTreeQueryDTO query);

    /**
     * 新增菜单/权限点（按钮下不可再建子节点，perms 唯一）。
     */
    MenuCreatedVO create(MenuCreateDTO dto);

    /**
     * 修改菜单（乐观锁；parentId 防环）。
     */
    MenuUpdatedVO update(Long id, MenuUpdateDTO dto);

    /**
     * 删除菜单（逻辑删除；子节点存在、已被角色引用不可删）。
     */
    DeleteResultVO delete(Long id);
}
