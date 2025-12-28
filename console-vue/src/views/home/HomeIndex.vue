<template>
  <div class="common-layout">
    <el-container>
      <el-header height="54px" style="padding: 0">
        <div class="header">
          <div @click="toMySpace" class="logo">SaaS短链接</div>
          <div style="display: flex; align-items: center">
            <el-dropdown>
              <div class="block">
                <span
                    class="name-span"
                    style="text-decoration: none"
                >{{username}}</span
                >
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="toMine">个人信息</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-main style="padding: 0">
        <div class="content-box">
          <RouterView class="content-space" />
        </div>
      </el-main>
      <!-- <el-container>
        <el-aside width="180px">
          <el-menu
            active-text-color="#073372"
            background-color="#0e5782"
            class="el-menu-vertical-demo"
            :default-active="getLasteRoute(route.path)"
            text-color="#fff"
            @select="handleSelect"
          >
            <template v-for="item in menuInfos" :key="item.name">
              <el-menu-item :index="item.path">
                <el-icon><icon-menu /></el-icon>
                <span>{{ item.name }}</span>
              </el-menu-item>
            </template>
          </el-menu></el-aside
        >

      </el-container> -->
    </el-container>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { removeKey, removeUsername, getToken, getUsername } from '@/core/auth.js'
import { ElMessage, ElNotification } from 'element-plus'
const { proxy } = getCurrentInstance()
const API = proxy.$API
// 当当前路径和菜单不匹配时，菜单不会被选中
const router = useRouter()
const squareUrl = ref('https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png')
const toMine = () => {
  router.push('/home' + '/account')
}
// 登出
const logout = async () => {
  const token = getToken()
  const username = getUsername()
  // 请求登出的接口
  await API.user.logout({ token, username })
  // 删除cookies中的token和username
  removeUsername()
  removeKey()
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
  ElMessage.success('成功退出！')
}
// 点击左上方的图片跳转到我的空间
const toMySpace = () => {
  router.push('/home' + '/space')
}
const username = ref('')
onMounted(async () => {
  const actualUsername = getUsername()
  const res = await API.user.queryUserInfo(actualUsername)
  // firstName.value = res?.data?.data?.realName?.split('')[0]
  username.value = truncateText(actualUsername, 8)
  
  // 检查是否已经弹出过提示
  const hasShownNotice = sessionStorage.getItem('hasShownWarning')
  if (!hasShownNotice) {
    ElNotification({
      title: '演示环境限制提示',
      message: '为了防止恶意跳转一些非法的网站，演示模式只支持以下：哔哩哔哩动画，知乎，小红书，掘金，博客园, 豆瓣',
      type: 'warning',
      duration: 6000,
      position: 'top-right',
      offset: 60
    })
    // sessionStorage.setItem('hasShownWarning', 'false')
  }
})
const extractColorByName = (name) => {
  var temp = []
  temp.push('#')
  for (let index = 0; index < name.length; index++) {
    temp.push(parseInt(name[index].charCodeAt(0), 10).toString(16))
  }
  return temp.slice(0, 5).join('').slice(0, 4)
}

// 辅助函数，用于截断文本
const truncateText = (text, maxLength) => {
  return text.length > maxLength ? text.slice(0, maxLength) + '...' : text
}
</script>

<style lang="scss" scoped>
.el-container {
  height: 100vh;
}

.header {
  background-color: #ffffff;
  padding: 0 20px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0; /* Slate 200 */
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);

  .block {
    cursor: pointer;
    display: flex;
    align-items: center;
    border: 0px;
  }
}

.content-box {
  height: calc(100vh - 54px);
  background-color: #f1f5f9; /* Slate 100 */
}

:deep(.el-tooltip__trigger:focus-visible) {
  outline: unset;
}

.logo {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a; /* Slate 900 - 更沉稳、专业的深色，避免廉价感 */
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  cursor: pointer;
  transition: opacity 0.2s;
  letter-spacing: -0.03em; /* 更紧凑的字间距，更有Logo感 */
  display: flex;
  align-items: center;

  /* 添加一个简单的文字高亮或图形感，如果需要的话。
     目前保持纯文字，但通过颜色区分 "SaaS" */
  &::before {
    content: '';
    display: inline-block;
    width: 20px;
    height: 20px;
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    border-radius: 4px;
    margin-right: 8px;
    mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71'/%3E%3Cpath d='M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71'/%3E%3C/svg%3E") no-repeat center/contain;
    -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='3' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71'/%3E%3Cpath d='M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71'/%3E%3C/svg%3E") no-repeat center/contain;
    background-color: #2563eb; /* Fallback */
  }
}

.logo:hover {
  opacity: 0.8; /* 简单的透明度变化，比变色更高级 */
  color: #0f172a;
}

.name-span {
  color: #64748b; /* Slate 500 */
  margin-right: 12px;
  font-size: 14px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  cursor: pointer;
  text-decoration: none;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-weight: 500;
  transition: color 0.2s;

  &:hover {
    color: #334155; /* Slate 700 */
  }
}
</style>
