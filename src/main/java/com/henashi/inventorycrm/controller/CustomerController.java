package com.henashi.inventorycrm.controller;

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
@Tag(name = "Customer API", description = "客户相关操作接口")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerDataExchangeService customerDataExchangeService;

    @GetMapping("/search")
    public List<CustomerSearchOptionDTO> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit) {
        return customerService.searchCustomers(keyword, limit);
    }

    @GetMapping("/import/template")
    public ImportResultDTO importTemplate() {
        return customerDataExchangeService.getImportTemplateMeta();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultDTO importCustomers(@RequestParam("file") MultipartFile file) {
        return customerDataExchangeService.importCustomers(file);
    }

    @GetMapping("/statistics")
    public CustomerStatisticsDTO statistics() {
        return customerService.getStatistics();
    }

    @GetMapping("/export")
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
    public CustomerBatchStatusUpdateResultDTO batchUpdateStatus(
            @Valid @RequestBody CustomerBatchStatusUpdateDTO request) {
        return customerService.batchUpdateStatus(request);
    }

    @GetMapping("/{id}")
    public CustomerDTO find(
            @PathVariable("id") @NotNull @Min(1) Long id) {
        return customerService.findCustomerDTOById(id);
    }

    @PostMapping
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
    public CustomerDTO update(
            @RequestBody CustomerUpdateDTO dto,
            @PathVariable("id") @NotNull @Min(1) Long id) {
        return customerService.updateCustomer(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") @NotNull @Min(1) Long id) {
        customerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
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
