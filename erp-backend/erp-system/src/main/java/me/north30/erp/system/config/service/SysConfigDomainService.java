package me.north30.erp.system.config.service;

import java.util.Collection;
import java.util.Map;

/**
 * 系统参数领域基础服务（只读取数，跨域消费方唯一准入接口）。
 * <p>静默回落策略：参数不存在/停用/解析失败时返回 fallback 并 log.warn，
 * 消费方不必因配置缺失而中断业务；缓存（system:param:{configKey}）接入后在本层内部替换数据源，消费方无感。</p>
 */
public interface SysConfigDomainService {

    /**
     * 读取数字型参数（valueType=2）。
     *
     * @param configKey 参数键
     * @param fallback  参数不存在/停用/非数字时的回落值
     * @return 参数值或 fallback
     */
    Long longValue(String configKey, Long fallback);

    /**
     * 读取字符串型参数（valueType=1），返回前去除首尾空白。
     *
     * @param configKey 参数键
     * @param fallback  参数不存在/停用时的回落值
     * @return 参数值或 fallback
     */
    String stringValue(String configKey, String fallback);

    /**
     * 读取布尔型参数（valueType=3），"true"/"1" 为真，"false"/"0" 为假。
     *
     * @param configKey 参数键
     * @param fallback  参数不存在/停用/取值非法时的回落值
     * @return 参数值或 fallback
     */
    Boolean booleanValue(String configKey, Boolean fallback);

    /**
     * 批量读取启用状态的参数（valueType 不限，一条 IN 查询）。
     *
     * @param configKeys 参数键集合
     * @return key → trim 后原值；未命中/停用的 key 不出现在 Map 中
     */
    Map<String, String> values(Collection<String> configKeys);
}
