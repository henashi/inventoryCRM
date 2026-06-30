package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.annotation.RequirePermission;
import static com.henashi.inventorycrm.constants.Permissions.*;
import com.henashi.inventorycrm.dto.OrderCreateDTO;
import com.henashi.inventorycrm.dto.OrderDTO;
import com.henashi.inventorycrm.service.CustomerOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理")
public class CustomerOrderController {

    private final CustomerOrderService orderService;

    @GetMapping
    @Operation(summary = "订单列表")
    public Page<OrderDTO> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.list(PageRequest.of(page, size, Sort.by("orderTime").descending()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public OrderDTO getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(ORDERS_DELETE)
    @Operation(summary = "删除订单")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @RequirePermission(ORDERS_CREATE)
    @Operation(summary = "创建订单（含商品明细）")
    public ResponseEntity<OrderDTO> create(@Valid @RequestBody OrderCreateDTO dto) {
        OrderDTO saved = orderService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location).body(saved);
    }
}
