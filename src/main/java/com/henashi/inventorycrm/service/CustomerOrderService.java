package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.OrderCreateDTO;
import com.henashi.inventorycrm.dto.OrderDTO;
import com.henashi.inventorycrm.dto.OrderItemDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.OrderItemMapper;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.CustomerOrder;
import com.henashi.inventorycrm.pojo.OrderItem;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.CustomerOrderRepository;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.OrderItemRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemMapper itemMapper;

    public Page<OrderDTO> list(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toDTO);
    }

    public OrderDTO getById(Long id) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "订单不存在"));
        return toDTO(order);
    }

    @Transactional
    public void delete(Long id) {
        orderItemRepository.findByOrderId(id).forEach(orderItemRepository::delete);
        orderRepository.deleteById(id);
    }

    @Transactional
    public OrderDTO create(OrderCreateDTO dto) {
        Customer customer = customerRepository.findById(dto.customerId())
                .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", "客户不存在"));

        BigDecimal totalAmount = dto.items().stream()
                .map(i -> i.totalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = dto.discount() != null ? dto.discount() : BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.subtract(discount).max(BigDecimal.ZERO);

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setTotalAmount(totalAmount);
        order.setDiscount(discount);
        order.setFinalAmount(finalAmount);
        order.setOrderTime(LocalDateTime.now());
        order.setRemark(dto.remark());
        CustomerOrder saved = orderRepository.save(order);

        for (var itemDto : dto.items()) {
            OrderItem item = itemMapper.createToEntity(itemDto);
            item.setOrder(saved);
            item.setCustomer(customer);
            if (itemDto.productId() != null) {
                Product product = productRepository.findById(itemDto.productId()).orElse(null);
                item.setProduct(product);
                if (product != null) item.setProductName(product.getName());
            }
            item.setOrderTime(saved.getOrderTime());
            orderItemRepository.save(item);
        }

        log.info("订单创建成功: ID={}, 客户={}, 金额={}", saved.getId(), customer.getName(), finalAmount);
        return toDTO(saved);
    }

    private OrderDTO toDTO(CustomerOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemDTO> itemDTOs = items.stream().map(itemMapper::toDTO).collect(Collectors.toList());

        return new OrderDTO(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getTotalAmount(),
                order.getDiscount(),
                order.getFinalAmount(),
                order.getOrderTime(),
                order.getRemark(),
                itemDTOs,
                order.getCreatedTime()
        );
    }
}
