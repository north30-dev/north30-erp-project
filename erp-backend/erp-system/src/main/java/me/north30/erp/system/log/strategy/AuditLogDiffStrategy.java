package me.north30.erp.system.log.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * 审计日志变更字段解析策略（纯函数策略，无外部依赖）。
 * <p>对比 before_json/after_json 的键值差异（新增/删除/值变化均视为变更字段）；
 * JSON 解析失败仅告警不阻断详情查看。</p>
 */
@Slf4j
@Component
public class AuditLogDiffStrategy {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * 变更字段清单：对比 before_json/after_json 的键值差异。
     *
     * @param beforeJson 变更前 JSON（可空）
     * @param afterJson  变更后 JSON（可空）
     * @return 变更字段名列表（升序）
     */
    public List<String> resolveDiffFields(String beforeJson, String afterJson) {
        JsonNode before = parseJsonObject(beforeJson);
        JsonNode after = parseJsonObject(afterJson);
        if (before == null && after == null) {
            return List.of();
        }
        TreeSet<String> diff = new TreeSet<>();
        if (before != null) {
            collectDiff(before, after, diff);
        }
        if (after != null) {
            collectDiff(after, before, diff);
        }
        return new ArrayList<>(diff);
    }

    /**
     * 将 source 中与 target 有差异的字段名加入结果集（null 值与缺失字段视为等价）。
     */
    private void collectDiff(JsonNode source, JsonNode target, TreeSet<String> diff) {
        for (Map.Entry<String, JsonNode> entry : source.properties()) {
            String name = entry.getKey();
            JsonNode targetValue = target == null ? null : target.get(name);
            if (targetValue == null || targetValue.isNull()) {
                if (entry.getValue() != null && !entry.getValue().isNull()) {
                    diff.add(name);
                }
            } else if (!Objects.equals(entry.getValue(), targetValue)) {
                diff.add(name);
            }
        }
    }

    /**
     * 解析 JSON 对象，非法或非对象内容返回 null（日志数据不阻断详情查看，仅告警）。
     */
    private JsonNode parseJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            return node != null && node.isObject() ? node : null;
        } catch (JacksonException e) {
            log.warn("审计日志变更 JSON 解析失败，diffFields 跳过：{}", e.getMessage());
            return null;
        }
    }
}
