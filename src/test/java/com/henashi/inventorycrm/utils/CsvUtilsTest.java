package com.henashi.inventorycrm.utils;

import com.henashi.inventorycrm.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvUtilsTest {

    // ========== parseCsvLine ==========

    @Test
    @DisplayName("解析CSV行 — 普通逗号分隔")
    void parseCsvLineNormal() {
        List<String> result = CsvUtils.parseCsvLine("a,b,c");
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("解析CSV行 — 带双引号的字段")
    void parseCsvLineQuotedField() {
        List<String> result = CsvUtils.parseCsvLine("\"hello, world\",b,c");
        assertThat(result).containsExactly("hello, world", "b", "c");
    }

    @Test
    @DisplayName("解析CSV行 — 转义双引号")
    void parseCsvLineEscapedQuote() {
        List<String> result = CsvUtils.parseCsvLine("\"say \"\"hello\"\"\",b");
        assertThat(result).containsExactly("say \"hello\"", "b");
    }

    @Test
    @DisplayName("解析CSV行 — 空字段")
    void parseCsvLineEmptyFields() {
        List<String> result = CsvUtils.parseCsvLine("a,,c,");
        assertThat(result).containsExactly("a", "", "c", "");
    }

    @Test
    @DisplayName("解析CSV行 — 单字段")
    void parseCsvLineSingleField() {
        List<String> result = CsvUtils.parseCsvLine("only");
        assertThat(result).containsExactly("only");
    }

    @Test
    @DisplayName("解析CSV行 — 空行")
    void parseCsvLineEmpty() {
        List<String> result = CsvUtils.parseCsvLine("");
        assertThat(result).containsExactly("");
    }

    @Test
    @DisplayName("解析CSV行 — null 输入")
    void parseCsvLineNull() {
        List<String> result = CsvUtils.parseCsvLine(null);
        assertThat(result).containsExactly("");
    }

    @Test
    @DisplayName("解析CSV行 — 开头结尾的双引号字段")
    void parseCsvLineLeadingTrailingQuotes() {
        List<String> result = CsvUtils.parseCsvLine("\"leading\",middle,\"trailing\"");
        assertThat(result).containsExactly("leading", "middle", "trailing");
    }

    // ========== readCsvRows ==========

    @Test
    @DisplayName("读取CSV — 正常多行")
    void readCsvRowsNormal(@Mock MultipartFile file) throws Exception {
        String csv = "name,phone\nAlice,13800138000\nBob,13900139000";
        InputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        when(file.getInputStream()).thenReturn(is);

        List<List<String>> rows = CsvUtils.readCsvRows(file, "ERROR");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0)).containsExactly("name", "phone");
        assertThat(rows.get(1)).containsExactly("Alice", "13800138000");
    }

    @Test
    @DisplayName("读取CSV — 文件读取失败抛 BusinessException")
    void readCsvRowsIOException(@Mock MultipartFile file) throws Exception {
        when(file.getInputStream()).thenThrow(new java.io.IOException("模拟IO异常"));

        assertThatThrownBy(() -> CsvUtils.readCsvRows(file, "MY_ERROR"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("导入文件解析失败");
    }

    // ========== buildHeaderIndex ==========

    @Test
    @DisplayName("构建表头索引 — 正常")
    void buildHeaderIndexNormal() {
        List<String> header = List.of("name", "phone", "email");
        Map<String, Integer> index = CsvUtils.buildHeaderIndex(header, List.of("name", "phone"), "ERR");
        assertThat(index).containsEntry("name", 0);
        assertThat(index).containsEntry("phone", 1);
        assertThat(index).containsEntry("email", 2);
    }

    @Test
    @DisplayName("构建表头索引 — 缺少必填字段抛异常")
    void buildHeaderIndexMissingRequired() {
        List<String> header = List.of("name", "email");
        assertThatThrownBy(() -> CsvUtils.buildHeaderIndex(header, List.of("name", "phone"), "HEADER_MISSING"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("phone");
    }

    @Test
    @DisplayName("构建表头索引 — BOM 头被清除")
    void buildHeaderIndexWithBom() {
        List<String> header = List.of("\uFEFFname", "phone");
        Map<String, Integer> index = CsvUtils.buildHeaderIndex(header, List.of("name"), "ERR");
        assertThat(index).containsKey("name");
    }

    @Test
    @DisplayName("构建表头索引 — 空表头列跳过")
    void buildHeaderIndexBlankColumn() {
        List<String> header = List.of("name", "", "phone");
        Map<String, Integer> index = CsvUtils.buildHeaderIndex(header, List.of("name", "phone"), "ERR");
        assertThat(index).doesNotContainKey("");
    }

    // ========== getCell ==========

    @Test
    @DisplayName("获取单元格 — 正常获取")
    void getCellNormal() {
        List<String> row = List.of("Alice", "13800138000");
        Map<String, Integer> index = Map.of("name", 0, "phone", 1);
        assertThat(CsvUtils.getCell(row, index, "name")).isEqualTo("Alice");
        assertThat(CsvUtils.getCell(row, index, "phone")).isEqualTo("13800138000");
    }

    @Test
    @DisplayName("获取单元格 — 字段不存在返回空串")
    void getCellMissingField() {
        List<String> row = List.of("Alice");
        Map<String, Integer> index = Map.of("name", 0);
        assertThat(CsvUtils.getCell(row, index, "phone")).isEqualTo("");
    }

    @Test
    @DisplayName("获取单元格 — 列号越界返回空串")
    void getCellIndexOutOfBounds() {
        List<String> row = List.of("Alice");
        Map<String, Integer> index = Map.of("name", 0, "phone", 5);
        assertThat(CsvUtils.getCell(row, index, "phone")).isEqualTo("");
    }

    // ========== isBlankRow ==========

    @Test
    @DisplayName("空行检测 — 全空行")
    void isBlankRowTrue() {
        assertThat(CsvUtils.isBlankRow(List.of("", "", "  "))).isTrue();
        assertThat(CsvUtils.isBlankRow(List.of())).isTrue();
    }

    @Test
    @DisplayName("空行检测 — 非空行")
    void isBlankRowFalse() {
        assertThat(CsvUtils.isBlankRow(List.of("", "data", ""))).isFalse();
    }

    // ========== normalizeHeader / normalizeText ==========

    @Test
    @DisplayName("标准化表头 — 去除 BOM 和空格")
    void normalizeHeader() {
        assertThat(CsvUtils.normalizeHeader("  name  ")).isEqualTo("name");
        assertThat(CsvUtils.normalizeHeader("\uFEFFname")).isEqualTo("name");
        assertThat(CsvUtils.normalizeHeader("")).isEqualTo("");
    }

    @Test
    @DisplayName("标准化文本 — null 转空串并 trim")
    void normalizeText() {
        assertThat(CsvUtils.normalizeText(null)).isEqualTo("");
        assertThat(CsvUtils.normalizeText("  hello  ")).isEqualTo("hello");
        assertThat(CsvUtils.normalizeText("")).isEqualTo("");
    }

    // ========== csv (反向转义，用于导出) ==========

    @Test
    @DisplayName("CSV 转义 — 普通值加双引号")
    void csvNormal() {
        assertThat(CsvUtils.csv("hello")).isEqualTo("\"hello\"");
    }

    @Test
    @DisplayName("CSV 转义 — 含双引号的值被转义")
    void csvWithQuotes() {
        assertThat(CsvUtils.csv("say \"hello\"")).isEqualTo("\"say \"\"hello\"\"\"");
    }

    @Test
    @DisplayName("CSV 转义 — null 转空串")
    void csvNull() {
        assertThat(CsvUtils.csv(null)).isEqualTo("");
    }

    @Test
    @DisplayName("CSV 转义 — 数字值")
    void csvNumber() {
        assertThat(CsvUtils.csv(123)).isEqualTo("\"123\"");
    }
}
