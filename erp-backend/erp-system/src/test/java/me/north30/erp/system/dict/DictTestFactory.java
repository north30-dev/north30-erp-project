package me.north30.erp.system.dict;

import me.north30.erp.system.dict.dto.DictItemCreateDTO;
import me.north30.erp.system.dict.dto.DictItemQueryDTO;
import me.north30.erp.system.dict.dto.DictItemUpdateDTO;
import me.north30.erp.system.dict.dto.DictTypeCreateDTO;
import me.north30.erp.system.dict.dto.DictTypeQueryDTO;
import me.north30.erp.system.dict.dto.DictTypeUpdateDTO;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.entity.SysDictType;

/**
 * 字典域测试数据静态工厂：集中构造实体与 DTO，避免测试方法内堆砌字段。
 */
public final class DictTestFactory {

    private DictTestFactory() {
    }

    /**
     * 构建字典类型实体（默认启用，备注为空）。
     */
    public static SysDictType dictType(Long id, String dictType, String dictName, Integer status) {
        SysDictType type = new SysDictType();
        type.setId(id);
        type.setDictType(dictType);
        type.setDictName(dictName);
        type.setStatus(status);
        return type;
    }

    /**
     * 构建字典项实体（默认启用，样式/扩展/备注为空）。
     */
    public static SysDictItem dictItem(Long id, String dictType, String itemLabel, String itemValue,
                                       String lang, Integer itemSort) {
        SysDictItem item = new SysDictItem();
        item.setId(id);
        item.setDictType(dictType);
        item.setItemLabel(itemLabel);
        item.setItemValue(itemValue);
        item.setLang(lang);
        item.setItemSort(itemSort);
        item.setIsDefault(0);
        item.setStatus(1);
        return item;
    }

    /**
     * 新增字典类型请求 DTO。
     */
    public static DictTypeCreateDTO typeCreateDTO(String dictType, String dictName, Integer status, String remark) {
        return new DictTypeCreateDTO(dictType, dictName, status, remark);
    }

    /**
     * 修改字典类型请求 DTO。
     */
    public static DictTypeUpdateDTO typeUpdateDTO(String dictName, Integer status, String remark, Integer version) {
        return new DictTypeUpdateDTO(dictName, status, remark, version);
    }

    /**
     * 字典类型分页查询参数。
     */
    public static DictTypeQueryDTO typeQueryDTO(String dictType, String dictName, Integer status,
                                                long pageNum, long pageSize) {
        DictTypeQueryDTO query = new DictTypeQueryDTO();
        query.setDictType(dictType);
        query.setDictName(dictName);
        query.setStatus(status);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return query;
    }

    /**
     * 新增字典项请求 DTO（样式/是否默认/扩展/备注固定为 null 以触发服务端默认值）。
     */
    public static DictItemCreateDTO itemCreateDTO(String dictType, String itemLabel, String itemValue,
                                                  String lang, Integer itemSort, Integer status) {
        return new DictItemCreateDTO(dictType, itemLabel, itemValue, lang, itemSort, null, null, null, status, null);
    }

    /**
     * 修改字典项请求 DTO（未传字段表示不修改，itemValue 不在修改范围）。
     */
    public static DictItemUpdateDTO itemUpdateDTO(Integer version, String itemLabel, Integer itemSort, Integer status) {
        return new DictItemUpdateDTO(itemLabel, itemSort, null, null, null, status, null, version);
    }

    /**
     * 字典项分页查询参数。
     */
    public static DictItemQueryDTO itemQueryDTO(String dictType, String lang, Integer status,
                                                long pageNum, long pageSize) {
        DictItemQueryDTO query = new DictItemQueryDTO();
        query.setDictType(dictType);
        query.setLang(lang);
        query.setStatus(status);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return query;
    }
}
