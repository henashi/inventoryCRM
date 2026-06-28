package com.henashi.inventorycrm.utils;

import com.henashi.inventorycrm.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CsvUtils {

    private CsvUtils() {}

    public static List<List<String>> readCsvRows(MultipartFile file, String errorCode) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<List<String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                rows.add(parseCsvLine(line));
            }
            return rows;
        } catch (IOException ex) {
            throw new BusinessException(errorCode, "导入文件解析失败，请检查文件编码或内容格式");
        }
    }

    public static Map<String, Integer> buildHeaderIndex(List<String> headerRow, List<String> requiredFields, String errorCode) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int columnIndex = 0; columnIndex < headerRow.size(); columnIndex++) {
            String header = normalizeHeader(headerRow.get(columnIndex));
            if (!header.isBlank()) {
                headerIndex.put(header, columnIndex);
            }
        }
        List<String> missingHeaders = requiredFields.stream()
                .filter(requiredField -> !headerIndex.containsKey(requiredField))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new BusinessException(errorCode,
                    "导入模板缺少必填列: " + String.join(", ", missingHeaders));
        }
        return headerIndex;
    }

    public static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            values.add("");
            return values;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (currentChar == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }
        values.add(current.toString());
        return values;
    }

    public static String getCell(List<String> row, Map<String, Integer> headerIndex, String fieldName) {
        Integer columnIndex = headerIndex.get(fieldName);
        if (columnIndex == null || columnIndex >= row.size()) {
            return "";
        }
        return normalizeText(row.get(columnIndex));
    }

    public static boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(value -> value == null || value.trim().isEmpty());
    }

    public static String normalizeHeader(String header) {
        return normalizeText(header).replace("\uFEFF", "");
    }

    public static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    public static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    /**
     * 校验导入文件：非空、≤5MB、.csv 后缀
     */
    public static void validateImportFile(MultipartFile file, String errorPrefix) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(errorPrefix + "_FILE_EMPTY", "导入文件不能为空");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new BusinessException(errorPrefix + "_FILE_TOO_LARGE", "导入文件不能超过 5MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessException(errorPrefix + "_FILE_TYPE_INVALID", "仅支持导入 CSV 文件");
        }
    }

    /**
     * 构建参数校验失败消息，多个错误用中文分号连接
     */
    public static String buildValidationMessage(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("；"));
    }
}
