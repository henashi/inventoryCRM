package com.henashi.inventorycrm;

import com.henashi.inventorycrm.mapper.CustomerMapper;
import com.henashi.inventorycrm.dto.CustomerCreateDTO;
import com.henashi.inventorycrm.dto.CustomerDTO;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

//@SpringBootTest
public class MyTest {
//
//    @Autowired
//    CustomerService customerService;
//    @Autowired
//    private CustomerRepository customerRepository;
//    @Autowired
//    private CustomerMapper customerMapper;
//
//    @Test
//    public void testCreateCustomer() {
//        CustomerCreateDTO customerCreateDTO = new CustomerCreateDTO("张二炮", "18123234568", "这是一串地址", null, 1, LocalDate.now(), null, null, null, null, null, 0, LocalDate.of(2000, 1, 1));
//        CustomerDTO customerDTO = customerService.saveCustomer(customerCreateDTO);
//        System.out.println(customerDTO);
//    }
//
//    @Test
//    @Transactional
//    public void testFind() {
//        CustomerDTO customerDTOById = customerService.findCustomerDTOById(1L);
//        System.out.println(customerDTOById);
//    }
//
//
////    @Test
////    @Commit
////    @Transactional
////    public void testUpdate() {
////        CustomerDTO customerDTOById = customerService.findCustomerDTOById(1L);
////        Customer entity = customerDTOById.toEntity(customerRepository);
////        entity.setAddress("浙江省嘉兴市");
//////        entity.setEmail("DapaoZhang@gmail.com");
////        customerService.updateCustomer(1L, customerMapper.updateFromEntity(entity));
////        System.out.println(1);
////    }
//
//    @Test
//    @Commit
//    @Transactional
//    public void testD() {
//        customerService.deleteById(10086L);
//    }
}
