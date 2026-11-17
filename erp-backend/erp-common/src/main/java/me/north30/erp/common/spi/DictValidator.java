package me.north30.erp.common.spi;

/**
 * 字典项校验 SPI（erp-common 抽象，由业务模块提供实现）。
 * <p>实现位于 erp-system 字典域；业务域（如 erp-base 物料计量单位 unit 字典校验）
 * 只依赖本抽象接口，跨模块不得直接注入 system 的 Service/Mapper。</p>
 */
public interface DictValidator {

    /**
     * 校验字典项必须存在且启用（类型存在、类型启用、项启用三重守卫），失败抛 {@code BusinessException}。
     *
     * @param dictType  字典类型编码
     * @param itemValue 字典项值
     */
    void requireEnabledItem(String dictType, String itemValue);
}
