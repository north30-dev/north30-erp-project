package me.north30.erp.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.text.csv.CsvParser;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvWriteConfig;
import cn.hutool.core.text.csv.CsvWriter;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

/**
 * CSV 读写统一封装（委托 Hutool CsvWriter/CsvParser，全项目唯一 CSV 方案）：
 * UTF-8 BOM + 逗号分隔 + 双引号转义 + CRLF 行尾，Excel 兼容口径。
 * <p>写：{@link #toCsvBytes(List, List)} 输出带 BOM 的 CSV 字节（导出/模板）；
 * 读：{@link #readRows(java.io.InputStream)} 逐行解析（含表头行，调用方自行校验与跳过）。</p>
 */
public final class CsvUtil {

    /** CSV UTF-8 BOM（Excel 兼容，标识编码） */
    public static final String BOM = "\uFEFF";

    private CsvUtil() {
    }

    /**
     * 表头 + 数据行 → CSV 字节（UTF-8 BOM，字段含逗号/引号/换行时自动双引号包裹转义）。
     *
     * @param header 表头字段
     * @param rows   数据行集合（每行字段数应与表头一致）
     * @return CSV 文件内容字节
     */
    public static byte[] toCsvBytes(List<String> header, List<List<String>> rows) {
        StringWriter out = new StringWriter();
        out.write(BOM);
        // 行尾显式指定 CRLF（Hutool 默认行为随版本变化，统一为 Windows/Excel 兼容口径）
        CsvWriter writer = new CsvWriter(out, CsvWriteConfig.defaultConfig()
            .setLineDelimiter(new char[]{'\r', '\n'}));
        try {
            writer.writeLine(header.toArray(new String[0]));
            for (List<String> row : rows) {
                writer.writeLine(row.toArray(new String[0]));
            }
        } finally {
            writer.close();
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 解析输入流为行集合（首行表头包含在内，调用方自行校验列数与跳过；
     * 字段支持双引号包裹与内部引号转义 ""）。
     *
     * @param in 输入流（UTF-8，可含 BOM）
     * @return 行集合（每行为字段列表）
     * @throws BusinessException 读取失败抛 10001
     */
    public static List<List<String>> readRows(InputStream in) {
        List<List<String>> rows = new ArrayList<>();
        try (CsvParser parser = new CsvParser(new InputStreamReader(in, StandardCharsets.UTF_8),
            CsvReadConfig.defaultConfig())) {
            boolean firstRow = true;
            CsvRow row;
            while ((row = parser.nextRow()) != null) {
                List<String> fields = row.getRawList();
                if (firstRow && !fields.isEmpty()) {
                    // Reader 方式不带 BOM 识别，手工剥离首行首字段 BOM
                    fields.set(0, fields.get(0).replace(BOM, ""));
                    firstRow = false;
                }
                rows.add(fields);
            }
        } catch (IOException e) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "CSV 文件读取失败：" + e.getMessage());
        }
        return rows;
    }
}
