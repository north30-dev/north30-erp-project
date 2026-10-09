package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.mapper.SysDictItemMapper;
import me.north30.erp.system.dict.service.SysDictItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典项基础服务实现（只读统计，管理端编排见 {@link DictManagementServiceImpl}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictItemServiceImpl implements SysDictItemService {

    private final SysDictItemMapper sysDictItemMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByTypes(List<String> dictTypes) {
        if (dictTypes.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<SysDictItem> countWrapper = new QueryWrapper<SysDictItem>()
            .select("dict_type", "count(1) AS cnt")
            .in("dict_type", dictTypes)
            .groupBy("dict_type");
        List<Map<String, Object>> rows = sysDictItemMapper.selectMaps(countWrapper);
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object dictType = row.get("dict_type");
            Object cnt = row.get("cnt");
            if (dictType != null && cnt != null) {
                counts.put(dictType.toString(), ((Number) cnt).longValue());
            }
        }
        return counts;
    }
}
