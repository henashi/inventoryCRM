package com.henashi.inventorycrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
// hateoas开启注解
//@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class InventoryCrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryCrmApplication.class, args);
	}

}
