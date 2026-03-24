<template>
  <div class="gift-log-page">
    <div class="page-header">
      <h1 class="page-title">礼品发放</h1>
      <div class="page-actions">
        <a-button @click="handleRefresh" :loading="isLoading">
          <reload-outlined />
          刷新
        </a-button>
        <a-button @click="handleAdd" type="primary" style="margin-left:8px">
          <gift-outlined />
          发放
        </a-button>
        <a-button @click="handleBack" style="margin-left:8px">
          <left-outlined />
          返回
        </a-button>
      </div>
    </div>
    <a-card class="table-card">
      <a-table
        :columns="columns"
        ref="formRef"
        :data-source="dataSource"
        :pagination="pagination"
        :loading="isLoading"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        @change="loadGiftLogs"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'gift'">
            {{ record.gift.name }}
          </template>
          <template v-else-if="column.dataIndex === 'customer'">
            {{ record.customer.name }}
          </template>
          <template v-else-if="column.dataIndex === 'quantity'">
            {{ record.quantity }}
          </template>
          <template v-else-if="column.dataIndex === 'createdTime'">
            {{ formatDateTime(record.createdTime) }}
          </template>
          <template v-else-if="column.dataIndex === 'issueTime'">
            {{ formatDateTime(record.issueTime) }}
          </template>
          <template v-else-if="column.dataIndex === 'issueNotes'">
            <a-tooltip :title="record.issueNotes">
              <span>{{ record.issueNotes }}</span>
            </a-tooltip>
          </template>
        </template>
        <template v-slot:action="{ record }">
          <a-button type="link" @click="router.push(`/gift/${record.id}`)">
            查看详情
          </a-button>
          <a-button
            type="link"
            size="small"
            @click="handleIssuePendingLog(record)"
            v-if="record.status === 'PENDING'"
          >
            发放
          </a-button>
        </template>

      </a-table>
    </a-card>
    <a-modal
      v-model:visible="modalVisible"
      title="发放礼品"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="关联礼品" name="giftId">
          <a-select
            v-model:value="formState.giftId"
            placeholder="请选择礼品"
            allow-clear:value="{{ formState.giftId===null }}"
            show-search:value="{{ formState.giftId===null }}"
            :filter-option="filterGiftOption"
            :disabled="modelType==='edit'"
            @-change="handleGiftChange"
          >
            <a-select-option v-for="gift in giftOptions" :key="gift.id" :value="gift.id">
              {{ gift.name }} {{ gift.code }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="领取人" name="customerId">
          <a-select
            v-model:value="formState.customerId"
            placeholder="请选择领取人"
            allow-clear:value="{{ formState.customerId===null }}"
            show-search:value="{{ formState.customerId===null }}"
            :filter-option="filterCustomerOption"
            :disabled="modelType==='edit'"
          >
            <a-select-option v-for="customer in customerOptions" :key="customer.id" :value="customer.id">
              {{ customer.name }} {{ customer.phone }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="发放数量" name="quantity">
          <a-input-number v-model:value="formState.quantity" style="width: 100%"
            :disabled="formState.limitEnabled" />
        </a-form-item>
        <a-form-item label="发放说明" name="issueNotes">
          <a-textarea v-model:value="formState.issueNotes" placeholder="请输入发放说明" :maxlength="50" show-count/>
        </a-form-item>
        <!-- <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formState.remark" placeholder="请输入备注信息" :maxlength="200" show-count/>
        </a-form-item> -->
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import dayjs from 'dayjs'
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useGiftStore } from '@/stores/gift';
import { useCustomerStore } from '@/stores/customer';
import { useGiftLogStore } from '@/stores/giftLog';
import { message } from 'ant-design-vue';
import type { FormInstance } from 'ant-design-vue'
import type { GiftLogDTO, PageParams, Gift } from '@/types';
import { ReloadOutlined, GiftOutlined, LeftOutlined } from '@ant-design/icons-vue';

const router = useRouter();
const giftStore = useGiftStore();
const customerStore = useCustomerStore();
const giftLogStore = useGiftLogStore();
const isLoading = ref(false);
const modalVisible = ref(false);
const formRef = ref<FormInstance>()
const modelType = ref<'add' | 'edit'>('add');
const currentGiftLog = ref<GiftLogDTO | null>(null);
const currentGift = ref<Gift | null>(null);
const dataSource = computed(() => giftLogStore.giftLogList);
const pagination = reactive({
  ...giftLogStore.pagination, // 总条数
  showTotal: (total: number) => `共 ${total} 条`,
  showSizeChanger: true, // 显示可改变每页数量
  showQuickJumper: true, // 显示快速跳转
});

const columns = [
  { title: '礼品名称', dataIndex: 'giftName', key: 'giftName' },
  { title: '领取人', dataIndex: 'customerName', key: 'customerName' },
  { title: '发放时间', dataIndex: 'createdTime', key: 'createdTime' },
  { title: '发放数量', dataIndex: 'quantity', key: 'quantity' },
  { title: '处理说明', dataIndex: 'issueNotes', key: 'issueNotes' },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 200,
    slots: { customRender: 'action' },
  },
];

const formState = reactive<GiftLogDTO>({
  id: null, // ID
  giftId: null, // 礼品ID
  giftName: '', // 礼品名称
  customerId: null, // 领取人ID
  customerName: '', // 领取人姓名
  quantity: 1, // 发放数量
  issueNotes: '', // 处理说明
  remark: '', // 备注
  issuedAt: '', // 发放时间
  operator: '', // 操作人
  status: 'PENDING', // 状态
  createdTime: '', // 创建时间
  updatedTime: '', // 更新时间
  limitEnabled: false, // 是否限制
});

const rules = {
  giftId: [{ required: true, message: '请选择关联礼品', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择领取人', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入发放数量', trigger: 'change' }],
  issueNotes: [
    // { required: true, message: '请输入处理说明', trigger: 'blur' },
    { max: 50, message: '处理说明不能超过50字', trigger: 'blur' },
  ]
};

const handleRefresh = () => {
  loadGiftLogs();
};

const handleAdd = () => {
  modelType.value = 'add';
  modalVisible.value = true;
};

const handleIssuePendingLog = (record: GiftLogDTO) => {
  currentGiftLog.value = record;
  modelType.value = 'edit';
  modalVisible.value = true;
  const result = findGiftLimitData(record.giftId!);
  Object.assign(formState, {
    giftId: currentGiftLog.value?.giftId || null,
    customerId: currentGiftLog.value?.customerId || null,
    quantity: currentGiftLog.value?.quantity || 1,
    issueNotes: currentGiftLog.value?.issueNotes || '',
    remark: currentGiftLog.value?.remark || '',
    status: 'ISSUED',
    limitEnabled: result.limitEnabled,
  });
};

const handleBack = () => {
  router.push('/');
};

const handleGiftChange = (value: number) => {
  const result = findGiftLimitData(value);
  formState.limitEnabled = result.limitEnabled;
  formState.quantity = result.limitPerPerson;
  Object.assign(formState, {
    ...formState,
    limitEnabled: result.limitEnabled,
    quantity: result.limitPerPerson,
  });
};

const handleModalOk = async () => {
  try {
    if (!formRef.value) {
      message.error('表单未加载');
      return;
    }
    await formRef.value.validate();
    if (modelType.value === 'add') {
      await giftLogStore.createGiftLog(formState);
      message.success('礼品发放成功');
    } else {

      // 编辑逻辑（如果需要）
      await giftLogStore.updateGiftLog(currentGiftLog.value!.id, formState);
      message.success('礼品日志更新成功');
    }
    loadGiftLogs();
    modalVisible.value = false;
  } catch (error) {
    console.error('表单验证失败:', error);
    message.error('请检查表单输入');
  }
};

const handleModalCancel = () => {
  modalVisible.value = false;
  formRef.value?.resetFields();
};

const filterGiftOption = (input: string, option: any) => {
  return (
    option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
  );
};

const findGiftLimitData = (id: number) => {
  const response = giftStore.gifts.find(gift => gift.id === id);
  return {
    limitEnabled: response?.limitEnabled,
    limitPerPerson: response?.limitPerPerson
  }
};

const giftOptions = computed(() => {
  return giftStore.gifts.map(gift => ({
    id: gift.id,
    name: gift.name,
    code: gift.code,
  }));
});

const filterCustomerOption = (input: string, option: any) => {
  return (
    option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
  );
};

const formatDateTime = (dateStr: string) => {
  return formatDate(dateStr, 'YYYY-MM-DD HH:mm:ss')
}

const formatDate = (dateStr: string, format = 'YYYY-MM-dd') => {
  if (!dateStr) return ''
  return dayjs(dateStr).format(format)
}

const customerOptions = computed(() => {
  return customerStore.customers.map(customer => ({
    id: customer.id,
    name: customer.name,
    phone: customer.phone,
  }));
})

const loadGiftLogs = async (params?: PageParams) => {
  console.log('加载礼品数据，参数:', params)
  try {
    const queryParams: PageParams = {
      page: params?.page || 0,
      size: params?.size || 10,
    }
    isLoading.value = true;
    await giftLogStore.loadGiftLogs(queryParams);
  } catch (error) {
    message.error('加载礼品数据失败');
  }
  finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  loadGiftLogs();
  giftStore.loadGifts({ page: 0, size: 100 }); // 加载前100个礼品用于选择
  customerStore.loadCustomers({ page: 0, size: 100 }); // 加载前100个客户用于选择
});
</script>
<style scoped>
.gift-log-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
</style>
