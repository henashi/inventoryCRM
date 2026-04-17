<template>
  <div class="dataDict-page">
    <div class="page-header">
      <h1 class="page-title">配置管理</h1>
      <div class="page-actions">
          <a-button @click="handleRefresh" :loading="isLoading">
            <reload-outlined />
            刷新
          </a-button>
          <a-button type="primary" @click="showAddModal" style="margin-left:8px">
            <plus-outlined />
            新增配置
          </a-button>
          <a-button @click="handleBack" style="margin-left:8px">
            <template #icon>
              <home-outlined />
            </template>
            返回
          </a-button>
      </div>
    </div>
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :pagination="pagination"
        :loading="isLoading"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        @change="loadDataDicts"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'limitEnabled'">
            <a-tag :color="record.limitEnabled ? 'green' : 'red'">
              {{ record.limitEnabled ? '是' : '否' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeText(record.type) }}
            </a-tag>
          </template>
        </template>

        <template #action="{ record }">
          <a-button
            type="link"
            size="small"
            @click="handleDataDictEdit(record)"
          >
            编辑
          </a-button>
          <a-button type="link" @click="handleActiveOrDisable(record, true)" v-if="record.status === 'DICT_STATUS_ACTIVE'">
            失效
          </a-button>
          <a-button type="link" @click="handleActiveOrDisable(record, false)" v-if="record.status === 'DICT_STATUS_PAUSED'">
            生效
          </a-button>
          <a-button
            type="link"
            danger
            size="small"
            @click="handleDelete(record)"
          >
            删除
          </a-button>
        </template>
      </a-table>
    </a-card>
    <a-modal
      v-model:visible="modalVisible"
      :title="modalTitle"
      width="600px"
      @ok="handleAddOrEdit"
      @cancel="handleModalCancel"
      >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="分组名称" name="groupName">
          <a-input v-model:value="formState.groupName" placeholder="请输入分组名称" />
        </a-form-item>
        <a-form-item label="分组编码" name="groupCode">
          <a-input v-model:value="formState.groupCode" placeholder="请输入分组编码" />
        </a-form-item>
        <a-form-item label="配置名称" name="paramName">
          <a-input v-model:value="formState.paramName" placeholder="请输入配置名称" />
        </a-form-item>
        <a-form-item label="配置编码" name="paramCode">
          <a-input v-model:value="formState.paramCode" placeholder="请输入配置编码" />
        </a-form-item>
        <a-form-item label="配置值" name="paramValue">
          <a-input v-model:value="formState.paramValue" placeholder="请输入配置值" />
        </a-form-item>
        <!-- <a-form-item label="配置排序" name="paramValue">
          <a-input v-model:value="formState.paramValue" placeholder="请输入排序值(数字越小展示越靠前)" />
        </a-form-item> -->
        <!-- <a-form-item label="状态" name="status">
          <a-select
            v-model:value="formState.status"
            placeholder="请选择状态类型"
            allow-clear>
            <a-select-option value="ACTIVE">生效</a-select-option>
            <a-select-option value="PAUSED">失效</a-select-option>
          </a-select>
        </a-form-item> -->
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" placeholder="请输入配置描述" :maxlength="200" show-count/>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import dayjs from 'dayjs'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDataDictStore } from '@/stores/dataDict'
import type { DataDict, DataDictDTO, PageParams } from '@/types'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { ReloadOutlined, HomeOutlined, PlusOutlined } from '@ant-design/icons-vue'

const modalType = ref<'add' | 'edit'>('add')
const modalTitle = ref('')
const modalVisible = ref(false)
const formRef = ref<FormInstance>()
const router = useRouter()
const dataDictStore = useDataDictStore()
const currentDataDict = ref<DataDict | null>(null)
const isLoading = ref(false);
const columns = [
  {
    title: '分组名称',
    dataIndex: 'groupName',
    key: 'groupName',
  },
  {
    title: '分组编码',
    dataIndex: 'groupCode',
    key: 'groupCode',
  },
  {
    title: '配置名称',
    dataIndex: 'paramName',
    key: 'paramName',
  },
  {
    title: '配置编码',
    dataIndex: 'paramCode',
    key: 'paramCode',
  },
  {
    title: '配置值',
    dataIndex: 'paramValue',
    key: 'paramValue',
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: (text: string) => {
      switch (text) {
        case 'DICT_STATUS_ACTIVE':
          return '生效'
        case 'DICT_STATUS_PAUSED':
          return '失效'
        default:
          return '未知'
      }
    }
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 200,
    slots: { customRender: 'action' },
  },
]
// 表单数据
const formState = reactive<DataDictDTO>({
  paramCode: null,
  paramName: null,
  paramValue: null,
  groupName: null,
  groupCode: null,
  description: null,
  status: 'ACTIVE',
  isDeleted: 0,
})

const rules = {
  groupName: [{ required: true, message: '请输入分组名称', trigger: 'blur' }],
  groupCode: [{ required: true, message: '请输入分组编码', trigger: 'blur' }],
  paramName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  paramCode: [{ required: true, message: '请输入配置编码', trigger: 'blur' }],
  paramValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
  // description: [{ required: true, message: '请输入配置描述', trigger: 'blur' }],
  // sortOrder: [{ required: true, message: '请选择排序类型', trigger: 'change' },
  // { pattern: /^[0-9]+$/, message: '排序类型必须为数字', trigger: 'blur' }],
}

const dataSource = computed(() => dataDictStore.dataDicts)
const pagination = computed(() => ({
  ...dataDictStore.pagination,
  showTotal: (total: number) => `共 ${total} 条数据`,
  showSizeChanger: true,
  showQuickJumper: true,
}))


const formatDate = (dateStr: string, format = 'YYYY-MM-dd') => {
  if (!dateStr) return ''
  return dayjs(dateStr).format(format)
}

const handleActiveOrDisable = (record: DataDict, isActive: boolean) => {
  const optionText = isActive ? '失效' : '生效'
  Modal.confirm({
    title: '确认',
    content: `确定要${optionText}配置 "${record.paramName}" 吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await dataDictStore.updateDataDictStatus(record.id, !isActive )
        message.success(`${optionText}成功`)
      } catch (error) {
        message.error(`${optionText}失败`)
      }
      finally {
        loadDataDicts()
      }
    }
  })

}

const handleDelete = (record: DataDict) => {
  Modal.confirm({
    title: '确认',
    content: `确定要删除配置 "${record.paramName}" 吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await dataDictStore.deleteDataDict(record.id)
        message.success('删除成功')
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
  loadDataDicts()
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'DICT_STATUS_ACTIVE':
      return 'green'
    case 'DICT_STATUS_PAUSED':
      return 'blue'
    default:
      return 'default'
  }
}

const handleAddOrEdit = async () => {
  try {
    if (!formRef.value) {
      message.error('表单未初始化')
      return
    }
    await formRef.value.validate()
    if (modalType.value === 'add') {
      await dataDictStore.createDataDict(formState)
      message.success('新增配置成功')
    } else {
      // 编辑逻辑（如果需要）
      await dataDictStore.updateDataDict(currentDataDict.value!.id, formState)
      message.success('更新配置成功')
    }
    modalVisible.value = false
  } catch (error) {
    console.error('表单验证失败:', error)
  }
  finally {
    formRef.value?.resetFields();
    loadDataDicts();
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  // 重置表单
  formRef.value?.resetFields();
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'DICT_STATUS_ACTIVE':
      return '生效'
    case 'DICT_STATUS_PAUSED':
      return '失效'
    default:
      return '未知'
  }
}

// 新增商品
const showAddModal = () => {
  modalType.value = 'add'
  Object.assign(formState, {
    paramCode: null,
    paramName: null,
    paramValue: null,
    groupName: null,
    groupCode: null,
    description: null,
    status: 'ACTIVE',
    isDeleted: 0,
  })

  modalVisible.value = true
  modalTitle.value = '新增配置'
}

const handleDataDictEdit = (dataDict: DataDict) => {
  currentDataDict.value = dataDict
  modalType.value = 'edit'
  Object.assign(formState, {
    paramName: dataDict.paramName,
    paramCode: dataDict.paramCode,
    paramValue: dataDict.paramValue,
    groupName: dataDict.groupName,
    groupCode: dataDict.groupCode,
    description: dataDict.description,
    status: dataDict.status,
  })
  modalVisible.value = true
  modalTitle.value = '编辑配置'
}

const handleRefresh = () => {
  loadDataDicts()
  message.success('刷新成功')
}

const handleBack = () => {
  router.push('/')
}

const loadDataDicts = async (params?: PageParams) => {
  console.log('加载配置数据，参数:', params)
  try {
    isLoading.value = true

    const queryParams: PageParams = {
      page: params?.page || 0,
      size: params?.size || 10,
    }

    await dataDictStore.loadDataDicts(queryParams)
  } catch (error) {
    message.error('加载配置数据失败')
  }
  finally {
    isLoading.value = false
  }
}

const handleViewDistributionLogs = () => {
  router.push('/dataDict-logs')
}

onMounted(() => {
  loadDataDicts()
})
</script>

<style scoped>
.dataDict-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.table-card {
  border-radius: 8px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .stats-cards {
    margin-bottom: 12px;
  }
}
</style>
