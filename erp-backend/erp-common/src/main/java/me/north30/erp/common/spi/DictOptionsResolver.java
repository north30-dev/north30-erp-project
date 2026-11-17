package me.north30.erp.common.spi;

import java.util.Map;

/**
 * 字典取值→标签解析 SPI：业务模块导出/展示场景经此读取数据字典启用项的标签，
 * 消除展示文案硬编码（字典运营改词后导出/展示自动跟随）。
 * <p>由 erp-system 字典域提供适配实现，bootstrap 装配（与 {@link DictValidator} 同机制）；
 * 跨模块禁止直接依赖 erp-system，只经本接口消费。</p>
 */
public interface DictOptionsResolver {

    /**
     * 加载指定字典类型的启用项 value→label 映射（一次查询，供导出/展示批量渲染，禁行级查询）。
     *
     * @param dictType 字典类型编码（sys_dict_type.dict_type）
     * @return 启用项 value → label 映射（无启用项时返回空映射，调用方按需回退默认文案）
     */
    Map<String, String> loadItemLabels(String dictType);
}
