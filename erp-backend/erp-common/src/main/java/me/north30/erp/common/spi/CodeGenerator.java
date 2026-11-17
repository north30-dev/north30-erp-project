package me.north30.erp.common.spi;

/**
 * 业务编码生成器 SPI（erp-common 抽象，由业务模块提供实现）。
 * <p>实现位于 erp-system codesequence 域（基于 sys_code_sequence 表原子发号），
 * 由 erp-bootstrap 装配暴露；业务域（如 erp-base 物料编码）只依赖本抽象接口，
 * 跨模块不得直接注入 system 的 Service/Mapper。</p>
 * <p>取号约定：以 (bizType, prefix) 为取号键按前缀隔离递增；返回"prefix + 6 位流水"，
 * 6 位流水用尽时抛出 {@code BusinessException}（流水用尽语义错误码）。</p>
 */
public interface CodeGenerator {

    /**
     * 按业务类型与前缀生成"prefix + 6 位流水"编码。
     *
     * @param bizType 业务类型（取号键之一，如 material_code）
     * @param prefix  编码前缀（取号键之二，流水按前缀隔离递增，如分类编码前 4 位）
     * @return 前缀 + 6 位流水组成的编码（如 0103000001）
     */
    String generate(String bizType, String prefix);
}
