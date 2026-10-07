package me.north30.erp.system.config.dto;

/**
 * 系统参数分页查询参数（接口文档 5.6.1）。
 * <p>pageNum/pageSize 缺省值由服务层 normalize 方法处理。</p>
 */
public record ConfigQueryDTO(
    String configKey,
    String configName,
    String configGroup,
    Integer status,
    Integer pageNum,
    Integer pageSize
) {
}
