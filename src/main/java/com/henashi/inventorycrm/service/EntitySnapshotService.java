package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.pojo.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntitySnapshotService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public <T extends BaseEntity> T getOldDataInNewTransaction(Class<T> entityClass, Long id) {
        if (id == null) return null;

        // 1. 从数据库重新加载（绕过主事务的一级缓存）
        T entity = entityManager.find(entityClass, id);

        // 2. 强制脱管（Detach），防止后续操作意外污染这个快照对象
        if (entity != null) {
            entityManager.detach(entity);
        }

        return entity;
    }

}
