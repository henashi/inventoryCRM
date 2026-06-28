package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.DataDict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DataDictRepositoryTest {

    @Autowired
    private DataDictRepository dataDictRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("customDeleteById — 软删除数据字典项")
    void customDeleteById() {
        // given: 创建一条数据字典记录
        DataDict dict = new DataDict();
        dict.setGroupCode("TEST_GROUP");
        dict.setGroupName("测试分组");
        dict.setParamCode("TEST_PARAM");
        dict.setParamName("测试参数");
        dict.setParamValue("test_value");
        DataDict saved = dataDictRepository.save(dict);
        assertNotNull(saved.getId());

        // when: 自定义删除
        int affected = dataDictRepository.customDeleteById(saved.getId());

        // then: 删除成功
        assertEquals(1, affected);
        entityManager.clear(); // 清除一级缓存，确保查询走 DB
        assertFalse(dataDictRepository.findById(saved.getId()).isPresent());
    }
}
