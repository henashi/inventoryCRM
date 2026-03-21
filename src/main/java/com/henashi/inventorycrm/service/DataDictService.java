package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.DataDictCreateDTO;
import com.henashi.inventorycrm.dto.DataDictDTO;
import com.henashi.inventorycrm.dto.DataDictUpdateDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.DataDictMapper;
import com.henashi.inventorycrm.pojo.DataDict;
import com.henashi.inventorycrm.repository.DataDictRepository;
import com.henashi.inventorycrm.utils.CustomBeanUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DataDictService {

    private final DataDictRepository dataDictRepository;

    private final DataDictMapper dataDictMapper;

    @Transactional(readOnly = true)
    public Page<DataDictDTO> findDataDictPage(Pageable pageable) {
        return dataDictRepository.findAll(pageable).map(dataDictMapper::fromEntity);
    }

    @Transactional(readOnly = true)
    public DataDictDTO findById(Long id) {
        if ( id == null || id <= 0 ) {
            throw new IllegalArgumentException("id无效，id = {}" + id);
        }
        return dataDictMapper.fromEntity(dataDictRepository.findById(id)
                .orElseThrow(() -> new BusinessException("id不合法：{}" + id)));
    }

    @Transactional
    public DataDictDTO createDataDict(DataDictCreateDTO dataDictCreateDTO) {
        DataDict entity = dataDictMapper.createToEntity(dataDictCreateDTO);
        entity.setIsDeleted(false);
        entity.setStatus(DataDict.DataDictStatus.DICT_STATUS_ACTIVE);
        DataDict dataDict = dataDictRepository.saveAndFlush(entity);
        return dataDictMapper.fromEntity(dataDict);
    }

    @Transactional
    public DataDictDTO updateDataDict(Long id, DataDictUpdateDTO dataDictUpdateDTO) {
        DataDict dataDictFind = dataDictRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("数据字典不存在: " + id));
        DataDict updateEntity = dataDictMapper.partialUpdate(dataDictUpdateDTO, dataDictFind);
        CustomBeanUtils.copyNonNullProperties(updateEntity, dataDictFind);
        DataDict dataDict = dataDictRepository.saveAndFlush(dataDictFind);
        return dataDictMapper.fromEntity(dataDict);
    }

    @Transactional
    public void deleteById(Long id) {
        int deleteById = dataDictRepository.customDeleteById(id);
        if (deleteById == 0) {
            throw new EntityNotFoundException("数据字典不存在: " + id);
        }
    }

    @Transactional(readOnly = true)
    public Optional<DataDict> findByGroupCodeAndParamCode(String groupCode, String paramCode) {
        if (!StringUtils.hasText(groupCode) && !StringUtils.hasText(paramCode)) {
            throw new IllegalArgumentException("查询配置参数无效：" + groupCode + "," + paramCode);
        }
        List<DataDict> dictList = dataDictRepository.findByGroupCodeAndParamCode(groupCode, paramCode);
        return dictList.isEmpty() ? Optional.empty() : Optional.of(dictList.get(0));
    }
}
