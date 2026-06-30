package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.annotation.RequirePermission;
import static com.henashi.inventorycrm.constants.Permissions.*;
import com.henashi.inventorycrm.dto.CustomerBatchStatusUpdateDTO;
import com.henashi.inventorycrm.dto.CustomerBatchStatusUpdateResultDTO;
import com.henashi.inventorycrm.dto.CustomerCreateDTO;
import com.henashi.inventorycrm.dto.CustomerDTO;
import com.henashi.inventorycrm.dto.CustomerSearchOptionDTO;
import com.henashi.inventorycrm.dto.CustomerStatisticsDTO;
import com.henashi.inventorycrm.dto.CustomerUpdateDTO;
import com.henashi.inventorycrm.dto.ImportResultDTO;
import com.henashi.inventorycrm.service.CustomerDataExchangeService;
import com.henashi.inventorycrm.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
@Tag(name = "客户管理", description = "客户 CRUD、搜索、导入导出、批量操作接口")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerDataExchangeService customerDataExchangeService;

    @GetMapping("/search")
    @Operation(summary = "客户搜索（补全）", description = "按关键字搜索客户，返回 ID/姓名/手机号 用于下拉补全")
    public List<CustomerSearchOptionDTO> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit) {
        return customerService.searchCustomers(keyword, limit);
    }

    @GetMapping("/import/template")
    @Operation(summary = "获取客户导入模板", description = "返回导入模板所需的字段列表、必填项、判重策略等元信息")
    public ImportResultDTO importTemplate() {
        return customerDataExchangeService.getImportTemplateMeta();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量导入客户", description = "上传 CSV 文件批量导入客户，按手机号判重")
    public ImportResultDTO importCustomers(@RequestParam("file") MultipartFile file) {
        return customerDataExchangeService.importCustomers(file);
    }

    @GetMapping("/statistics")
    @Operation(summary = "客户统计", description = "返回客户总数、正常/停用数量、礼品等级分布等统计数据")
    public CustomerStatisticsDTO statistics() {
        return customerService.getStatistics();
    }

    @GetMapping("/export")
    @Operation(summary = "导出客户 CSV", description = "按筛选条件导出客户列表为 CSV 文件")
    public ResponseEntity<byte[]> export(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "giftLevel", required = false) Integer giftLevel,
            @RequestParam(name = "gender", required = false) Integer gender,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate) {
        byte[] content = customerService.exportCustomers(keyword, status, giftLevel, gender, startDate, endDate);
        String fileName = "customers_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        return buildCsvResponse(fileName, content);
    }

    @PutMapping("/batch/status")
    @Operation(summary = "批量更新客户状态", description = "批量启用或停用指定客户")
    public CustomerBatchStatusUpdateResultDTO batchUpdateStatus(
            @Valid @RequestBody CustomerBatchStatusUpdateDTO request) {
        return customerService.batchUpdateStatus(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询客户详情", description = "根据 ID 查询客户详细信息")
    public CustomerDTO find(
            @PathVariable("id") @NotNull @Min(1) Long id) {
        return customerService.findCustomerDTOById(id);
    }

    @PostMapping
    @Operation(summary = "创建客户", description = "创建新客户，手机号不可重复")
    public ResponseEntity<CustomerDTO> create(@Valid @RequestBody CustomerCreateDTO customerCreateDTO) {
        CustomerDTO savedCustomer = customerService.createCustomer(customerCreateDTO);
        URI uri = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(savedCustomer.id())
                    .toUri();
        return ResponseEntity.created(uri).body(savedCustomer);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "更新客户", description = "部分更新客户信息，仅传需要修改的字段")
    public CustomerDTO update(
            @RequestBody CustomerUpdateDTO dto,
            @PathVariable("id") @NotNull @Min(1) Long id) {
        return customerService.updateCustomer(id, dto);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(CUSTOMERS_DELETE)
    @Operation(summary = "删除客户", description = "软删除客户（标记 deleted=true）")
    public ResponseEntity<Void> delete(@PathVariable("id") @NotNull @Min(1) Long id) {
        customerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "分页查询客户列表", description = "支持关键字搜索、状态/礼品等级/性别/日期范围筛选与排序")
    public Page<CustomerDTO> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "giftLevel", required = false) Integer giftLevel,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate,
            @RequestParam(name = "gender", required = false) Integer gender,
            @RequestParam(name = "direction", required = false) String direction) {
        return customerService.findAllCustomers(
                                PageRequest.of(page, size),
                                keyword, sort, status, giftLevel,
                                gender, direction, startDate, endDate);
    }

    private ResponseEntity<byte[]> buildCsvResponse(String fileName, byte[] content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(content.length)
                .body(content);
    }
}
