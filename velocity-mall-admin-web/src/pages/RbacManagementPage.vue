<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>权限管理</h1>
        <p>维护后台管理员、角色和接口权限绑定；权限码由后端内置，角色负责组合授权。</p>
      </div>
      <div class="tab-switch">
        <button :class="{ active: activeTab === 'admins' }" type="button" @click="activeTab = 'admins'">管理员</button>
        <button :class="{ active: activeTab === 'roles' }" type="button" @click="activeTab = 'roles'">角色权限</button>
      </div>
    </div>

    <template v-if="activeTab === 'admins'">
      <form class="filters panel" @submit.prevent="loadAdmins(1)">
        <label>
          关键词
          <input v-model.trim="adminFilters.keyword" placeholder="账号或姓名" />
        </label>
        <label>
          状态
          <select v-model="adminFilters.status">
            <option value="">全部</option>
            <option value="1">启用</option>
            <option value="0">禁用</option>
          </select>
        </label>
        <button class="primary-button" type="submit"><Search :size="17" /> 搜索</button>
        <button class="outline-button" type="button" @click="resetAdminFilters">重置</button>
        <button v-if="canWriteRbac" class="primary-button" type="button" @click="newAdmin"><Plus :size="17" /> 新建管理员</button>
      </form>

      <div class="drawer-grid" :class="{ 'single-column': !canWriteRbac }">
        <div class="panel">
          <table class="data-table">
            <thead>
              <tr>
                <th>账号</th>
                <th>姓名</th>
                <th>角色</th>
                <th>状态</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="admin in adminRecords" :key="admin.adminId">
                <td><strong>{{ admin.username }}</strong><br /><span class="subtext">ID {{ admin.adminId }}</span></td>
                <td>{{ admin.realName || '-' }}</td>
                <td>
                  <div class="tag-list">
                    <span v-for="role in admin.roles" :key="role.id" class="tag">{{ role.roleName }}</span>
                  </div>
                </td>
                <td><StatusBadge type="coupon" :value="admin.status" /></td>
                <td>{{ formatTime(admin.updateTime) }}</td>
                <td>
                  <div v-if="canWriteRbac" class="row-actions">
                    <button class="ghost-button compact" type="button" @click="editAdmin(admin)">编辑</button>
                    <button class="ghost-button compact" type="button" @click="resetPassword(admin)">重置密码</button>
                    <button class="danger-button compact" type="button" @click="toggleAdmin(admin)">
                      {{ admin.status === 1 ? '禁用' : '启用' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="!store.loading && adminRecords.length === 0" />
          <Pager
            v-if="store.adminPage"
            :page="store.adminPage.current"
            :pages="store.adminPage.pages"
            :total="store.adminPage.total"
            @change="loadAdmins"
          />
        </div>

        <aside v-if="canWriteRbac" class="panel">
          <div class="section-title"><h2>{{ editingAdminId ? '编辑管理员' : '新建管理员' }}</h2></div>
          <form class="panel-body form-grid" @submit.prevent="saveAdmin">
            <label class="field full">
              登录账号
              <input v-model.trim="adminForm.username" :disabled="Boolean(editingAdminId)" required />
            </label>
            <label v-if="!editingAdminId" class="field full">
              初始密码
              <input v-model.trim="adminForm.password" minlength="6" required type="password" />
            </label>
            <label class="field full">
              真实姓名
              <input v-model.trim="adminForm.realName" />
            </label>
            <label class="field full">
              状态
              <select v-model.number="adminForm.status">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </label>
            <div class="field full">
              <label>绑定角色</label>
              <div class="checkbox-list">
                <label v-for="role in enabledRoles" :key="role.id" class="checkbox-item">
                  <input v-model="adminForm.roleIds" type="checkbox" :value="role.id" />
                  <span>
                    <strong>{{ role.roleName }}</strong>
                    <small>{{ role.roleCode }}</small>
                  </span>
                </label>
              </div>
            </div>
            <div class="form-actions field full">
              <button class="primary-button" type="submit" :disabled="store.saving">保存</button>
              <button class="outline-button" type="button" @click="newAdmin">清空</button>
            </div>
            <div class="message field full" :class="{ success: Boolean(message), error: Boolean(error) }">
              {{ error || message || '管理员至少需要绑定一个启用角色。' }}
            </div>
          </form>
        </aside>
      </div>
    </template>

    <template v-else>
      <div class="drawer-grid" :class="{ 'single-column': !canWriteRbac }">
        <div class="panel">
          <div class="panel-body page-actions">
            <button v-if="canWriteRbac" class="primary-button" type="button" @click="newRole"><Plus :size="17" /> 新建角色</button>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>角色</th>
                <th>权限</th>
                <th>状态</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="role in roles" :key="role.id">
                <td>
                  <strong>{{ role.roleName }}</strong><br />
                  <span class="subtext">{{ role.roleCode }}</span>
                </td>
                <td>
                  <div class="tag-list">
                    <span v-for="permission in role.permissions" :key="permission.id" class="tag muted-tag">
                      {{ permission.permissionCode }}
                    </span>
                  </div>
                </td>
                <td><StatusBadge type="coupon" :value="role.status" /></td>
                <td>{{ formatTime(role.updateTime) }}</td>
                <td>
                  <div v-if="canWriteRbac" class="row-actions">
                    <button class="ghost-button compact" type="button" @click="editRole(role)">编辑</button>
                    <button class="danger-button compact" type="button" @click="toggleRole(role)">
                      {{ role.status === 1 ? '禁用' : '启用' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <EmptyState v-if="roles.length === 0" />
        </div>

        <aside v-if="canWriteRbac" class="panel">
          <div class="section-title"><h2>{{ editingRoleId ? '编辑角色' : '新建角色' }}</h2></div>
          <form class="panel-body form-grid" @submit.prevent="saveRole">
            <label class="field full">
              角色编码
              <input
                v-model.trim="roleForm.roleCode"
                :disabled="Boolean(editingRoleId)"
                placeholder="例如 CONTENT_STAFF"
                required
              />
            </label>
            <label class="field full">
              角色名称
              <input v-model.trim="roleForm.roleName" required />
            </label>
            <label class="field full">
              角色说明
              <textarea v-model.trim="roleForm.description" />
            </label>
            <label class="field full">
              状态
              <select v-model.number="roleForm.status" :disabled="isEditingSuperAdminRole">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </label>
            <div class="field full">
              <label>权限码</label>
              <div class="permission-groups">
                <section v-for="group in groupedPermissions" :key="group.resource" class="permission-group">
                  <h3>{{ group.resource }}</h3>
                  <div class="checkbox-list">
                    <label v-for="permission in group.permissions" :key="permission.id" class="checkbox-item">
                      <input
                        v-model="roleForm.permissionIds"
                        :disabled="isEditingSuperAdminRole"
                        type="checkbox"
                        :value="permission.id"
                      />
                      <span>
                        <strong>{{ permission.permissionName }}</strong>
                        <small>{{ permission.permissionCode }}</small>
                      </span>
                    </label>
                  </div>
                </section>
              </div>
            </div>
            <div class="form-actions field full">
              <button class="primary-button" type="submit" :disabled="store.saving">保存</button>
              <button class="outline-button" type="button" @click="newRole">清空</button>
            </div>
            <div class="message field full" :class="{ success: Boolean(message), error: Boolean(error) }">
              {{ error || message || (isEditingSuperAdminRole ? '超级管理员角色固定拥有全部权限。' : '角色至少需要选择一个权限码。') }}
            </div>
          </form>
        </aside>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Plus, Search } from 'lucide-vue-next';
import EmptyState from '@/components/EmptyState.vue';
import Pager from '@/components/Pager.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import type { AdminPermissionVO, AdminRoleVO, AdminUserVO } from '@/api/types';
import { AdminPermissions } from '@/constants/permissions';
import { useAdminAuthStore } from '@/stores/adminAuthStore';
import { useAdminRbacStore } from '@/stores/adminRbacStore';
import { formatTime } from '@/utils/format';

const auth = useAdminAuthStore();
const store = useAdminRbacStore();
const canWriteRbac = computed(() => auth.hasPermission(AdminPermissions.RBAC_WRITE));
const activeTab = ref<'admins' | 'roles'>('admins');
const message = ref('');
const error = ref('');

const adminFilters = reactive({ keyword: '', status: '' });
const editingAdminId = ref<string | null>(null);
const adminForm = reactive({
  username: '',
  password: '123456',
  realName: '',
  status: 1,
  roleIds: [] as string[]
});

const editingRoleId = ref<string | null>(null);
const roleForm = reactive({
  roleCode: '',
  roleName: '',
  description: '',
  status: 1,
  permissionIds: [] as string[]
});

const adminRecords = computed(() => store.adminPage?.records || []);
const roles = computed(() => store.roles);
const enabledRoles = computed(() => roles.value.filter((role) => role.status === 1));
const isEditingSuperAdminRole = computed(() => roleForm.roleCode === 'SUPER_ADMIN');
const groupedPermissions = computed(() => {
  const groups = new Map<string, AdminPermissionVO[]>();
  for (const permission of store.permissions) {
    if (!groups.has(permission.resource)) {
      groups.set(permission.resource, []);
    }
    groups.get(permission.resource)?.push(permission);
  }
  return Array.from(groups.entries()).map(([resource, permissions]) => ({ resource, permissions }));
});

async function loadAdmins(page = 1) {
  await store.loadAdmins({
    page,
    size: 10,
    keyword: adminFilters.keyword,
    status: adminFilters.status === '' ? null : Number(adminFilters.status)
  });
}

async function reloadReferenceData() {
  await Promise.all([store.loadRoles(), store.loadPermissions()]);
}

function clearFeedback() {
  message.value = '';
  error.value = '';
}

function resetAdminFilters() {
  adminFilters.keyword = '';
  adminFilters.status = '';
  void loadAdmins(1);
}

function newAdmin() {
  clearFeedback();
  editingAdminId.value = null;
  Object.assign(adminForm, {
    username: '',
    password: '123456',
    realName: '',
    status: 1,
    roleIds: [] as string[]
  });
}

function editAdmin(admin: AdminUserVO) {
  clearFeedback();
  editingAdminId.value = admin.adminId;
  Object.assign(adminForm, {
    username: admin.username,
    password: '',
    realName: admin.realName || '',
    status: admin.status,
    roleIds: admin.roles.map((role) => role.id)
  });
}

async function saveAdmin() {
  clearFeedback();
  if (adminForm.roleIds.length === 0) {
    error.value = '请至少选择一个角色';
    return;
  }
  try {
    if (editingAdminId.value) {
      await store.saveAdmin({
        realName: adminForm.realName,
        status: adminForm.status,
        roleIds: adminForm.roleIds
      }, editingAdminId.value);
    } else {
      await store.saveAdmin({
        username: adminForm.username,
        password: adminForm.password,
        realName: adminForm.realName,
        status: adminForm.status,
        roleIds: adminForm.roleIds
      });
    }
    message.value = '管理员已保存';
    await loadAdmins(store.adminPage?.current || 1);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '管理员保存失败';
  }
}

async function toggleAdmin(admin: AdminUserVO) {
  const next = admin.status === 1 ? 0 : 1;
  if (!window.confirm(`确认${next === 1 ? '启用' : '禁用'}管理员「${admin.username}」？`)) return;
  clearFeedback();
  try {
    await store.updateAdminStatus(admin.adminId, next);
    await loadAdmins(store.adminPage?.current || 1);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '状态更新失败';
  }
}

async function resetPassword(admin: AdminUserVO) {
  const password = window.prompt(`请输入「${admin.username}」的新密码`, '123456');
  if (!password) return;
  clearFeedback();
  try {
    await store.resetPassword(admin.adminId, password);
    message.value = '密码已重置';
  } catch (err) {
    error.value = err instanceof Error ? err.message : '密码重置失败';
  }
}

function newRole() {
  clearFeedback();
  editingRoleId.value = null;
  Object.assign(roleForm, {
    roleCode: '',
    roleName: '',
    description: '',
    status: 1,
    permissionIds: [] as string[]
  });
}

function editRole(role: AdminRoleVO) {
  clearFeedback();
  editingRoleId.value = role.id;
  Object.assign(roleForm, {
    roleCode: role.roleCode,
    roleName: role.roleName,
    description: role.description || '',
    status: role.status,
    permissionIds: role.permissions.map((permission) => permission.id)
  });
}

async function saveRole() {
  clearFeedback();
  if (!isEditingSuperAdminRole.value && roleForm.permissionIds.length === 0) {
    error.value = '请至少选择一个权限码';
    return;
  }
  try {
    await store.saveRole({
      roleCode: roleForm.roleCode.toUpperCase(),
      roleName: roleForm.roleName,
      description: roleForm.description,
      status: roleForm.status,
      permissionIds: roleForm.permissionIds
    }, editingRoleId.value || undefined);
    message.value = '角色已保存';
    await reloadReferenceData();
    await loadAdmins(store.adminPage?.current || 1);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '角色保存失败';
  }
}

async function toggleRole(role: AdminRoleVO) {
  const next = role.status === 1 ? 0 : 1;
  if (!window.confirm(`确认${next === 1 ? '启用' : '禁用'}角色「${role.roleName}」？`)) return;
  clearFeedback();
  try {
    await store.updateRoleStatus(role.id, next);
    await reloadReferenceData();
    await loadAdmins(store.adminPage?.current || 1);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '状态更新失败';
  }
}

onMounted(async () => {
  await reloadReferenceData();
  await loadAdmins(1);
});
</script>
