package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.DataDict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DataDictRepository extends JpaRepository<DataDict, Long> {

    @Modifying
    @Query("delete from DataDict where id = :id")
    int customDeleteById(@Param("id") Long id);

    List<DataDict> findByGroupCodeAndParamCode(String groupCode, String paramCode);
}
