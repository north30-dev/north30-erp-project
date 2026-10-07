package me.north30.erp.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 可访问仓库 ID 集合与逗号分隔字符串互转工具（纯静态，无 Spring 依赖）。
 * <p>序列化约定：null 不变、空集合存空串（显式清空语义）；解析时空串返回 null。</p>
 */
public final class WarehouseIdCodecUtil {

    private WarehouseIdCodecUtil() {
    }

    /**
     * 仓库 ID 集合序列化（逗号串）：null 返回 null，空集合返回空串。
     */
    public static String join(List<Long> warehouseIds) {
        if (warehouseIds == null) {
            return null;
        }
        return warehouseIds.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * 逗号串 → 仓库 ID 集合：null/空白返回 null，段两侧空格忽略，空段跳过。
     */
    public static List<Long> parse(String warehouseIds) {
        if (warehouseIds == null || warehouseIds.isBlank()) {
            return null;
        }
        return Arrays.stream(warehouseIds.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .map(Long::valueOf)
            .toList();
    }
}
