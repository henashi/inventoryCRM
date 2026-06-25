package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.OrderItemCreateDTO;
import com.henashi.inventorycrm.dto.OrderItemDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.OrderItemMapper;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.OrderItem;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.OrderItemRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemMapper mapper;

    public Page<OrderItemDTO> list(Pageable pageable) {
        return orderItemRepository.findAll(pageable).map(mapper::toDTO);
    }

    public Page<OrderItemDTO> listByCustomer(Long customerId, Pageable pageable) {
        return orderItemRepository.findByCustomerId(customerId, pageable).map(mapper::toDTO);
    }

    public OrderItemDTO getById(Long id) {
        return orderItemRepository.findById(id).map(mapper::toDTO)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "消费记录不存在"));
    }

    @Transactional
    public OrderItemDTO create(OrderItemCreateDTO dto) {
        Customer customer = customerRepository.findById(dto.customerId())
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "客户不存在"));

        OrderItem item = mapper.createToEntity(dto);
        item.setCustomer(customer);

        if (dto.productId() != null) {
            Product product = productRepository.findById(dto.productId()).orElse(null);
            item.setProduct(product);
            if (product != null) item.setProductName(product.getName());
        } else {
            item.setProductName(dto.productName());
        }

        item.setOrderTime(LocalDateTime.now());
        OrderItem saved = orderItemRepository.save(item);
        log.info("消费记录创建成功: 客户 {} 消费 {}", customer.getName(), dto.totalAmount());
        return mapper.toDTO(saved);
    }

    @Transactional
    public List<OrderItemDTO> batchCreate(List<OrderItemCreateDTO> dtos) {
        return dtos.stream().map(this::create).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        orderItemRepository.deleteById(id);
    }
}
