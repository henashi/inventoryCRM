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
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        entity.setDeleted(false);
        entity.setDictStatus(DataDict.DataDictStatus.DICT_STATUS_ACTIVE);
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
    public DataDictDTO enableOrDisable(Long id, @NotNull Boolean enable) {
        DataDict dataDictFind = dataDictRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("数据字典不存在: " + id));

        dataDictFind.setDictStatus(enable ? DataDict.DataDictStatus.DICT_STATUS_ACTIVE : DataDict.DataDictStatus.DICT_STATUS_PAUSED);
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

    @Transactional
    public void initPermissionDefaults() {
        String[][] defaults = {
            {"dashboard:view", "查看仪表盘", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"customers:view", "查看客户列表", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"customers:create", "新增客户", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"customers:edit", "编辑客户", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"customers:delete", "删除客户", "[\"ADMIN\"]"},
            {"customers:import", "导入客户", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"customers:export", "导出客户", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"products:view", "查看商品列表", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"products:create", "新增商品", "[\"ADMIN\",\"MANAGER\"]"},
            {"products:edit", "编辑商品", "[\"ADMIN\",\"MANAGER\"]"},
            {"products:delete", "删除商品", "[\"ADMIN\",\"MANAGER\"]"},
            {"products:import", "导入商品", "[\"ADMIN\",\"MANAGER\"]"},
            {"products:export", "导出商品", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"products:enable", "停用/启用商品", "[\"ADMIN\",\"MANAGER\"]"},
            {"inventory:view", "查看库存", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"inventory:stockIn", "入库", "[\"ADMIN\",\"MANAGER\"]"},
            {"inventory:stockOut", "出库", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"inventory:adjust", "调整库存", "[\"ADMIN\",\"MANAGER\"]"},
            {"inventory:export", "导出快照", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"orders:view", "查看订单", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"orders:create", "新增订单", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"orders:delete", "删除订单", "[\"ADMIN\"]"},
            {"gifts:view", "查看礼品", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"gifts:create", "新增礼品", "[\"ADMIN\",\"MANAGER\"]"},
            {"gifts:edit", "编辑礼品", "[\"ADMIN\",\"MANAGER\"]"},
            {"gifts:delete", "删除礼品", "[\"ADMIN\"]"},
            {"giftLogs:view", "查看发放日志", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"giftLogs:delete", "删除发放日志", "[\"ADMIN\"]"},
            {"dataDicts:view", "查看配置", "[\"ADMIN\"]"},
            {"dataDicts:manage", "管理配置", "[\"ADMIN\"]"},
            {"users:view", "查看用户", "[\"ADMIN\"]"},
            {"users:manage", "管理用户", "[\"ADMIN\"]"},
            {"operationLogs:view", "查看系统日志", "[\"ADMIN\",\"MANAGER\",\"USER\"]"},
            {"ai:scoring", "AI 客户评分", "[\"ADMIN\",\"MANAGER\"]"},
            {"ai:recommendation", "AI 礼品推荐", "[\"ADMIN\",\"MANAGER\"]"},
            {"ai:assistant", "AI 运营助手", "[\"ADMIN\",\"MANAGER\"]"},
            {"ai:prediction", "AI 库存预测", "[\"ADMIN\",\"MANAGER\"]"}
        };

        for (String[] def : defaults) {
            List<DataDict> existing = dataDictRepository
                    .findByGroupCodeAndParamCode("PERMISSION", def[0]);
            if (existing.isEmpty()) {
                DataDict dict = DataDict.builder()
                        .groupCode("PERMISSION")
                        .groupName("权限配置")
                        .paramCode(def[0])
                        .paramName(def[1])
                        .paramValue(def[2])
                        .description("权限默认值，格式为角色 JSON 数组")
                        .build();
                dataDictRepository.save(dict);
            }
        }
    }
}
