<template>
  <div class="login-page">
    <div class="background-blur"></div>
    <div class="content-wrapper">
      <h1 class="title">SaaS 短链接平台</h1>
      <div class="login-container">
        <div class="login-box">
          <!-- 登录 -->
          <div class="form-section logon" v-show="isLogin">
            <h2>欢迎回来</h2>
            <p class="subtitle">登录您的账号</p>
            <el-form ref="loginFormRef1" :model="loginForm" :rules="loginFormRule">
              <el-form-item prop="phone">
                <el-input
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  size="large"
                  clearable
                  prefix-icon="User">
                </el-input>
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  clearable
                  placeholder="请输入密码"
                  show-password
                  size="large"
                  prefix-icon="Lock">
                </el-input>
              </el-form-item>

              <div class="form-options">
                <el-checkbox v-model="checked">记住密码</el-checkbox>
              </div>

              <el-button
                :loading="loading"
                type="primary"
                size="large"
                class="login-btn"
                @click="login(loginFormRef1)">
                登录
              </el-button>

              <div class="switch-text">
                还没有账号？<span class="link-text" @click="changeLogin">立即注册</span>
              </div>
            </el-form>
          </div>

          <!-- 注册 -->
          <div class="form-section register" v-show="!isLogin">
            <h2>创建账号</h2>
            <p class="subtitle">开始您的短链接之旅</p>
            <el-form ref="loginFormRef2" :model="addForm" :rules="addFormRule">
              <el-form-item prop="username">
                <el-input
                  v-model="addForm.username"
                  placeholder="请输入用户名"
                  clearable
                  size="large"
                  prefix-icon="User">
                </el-input>
              </el-form-item>

              <el-form-item prop="realName">
                <el-input
                  v-model="addForm.realName"
                  placeholder="请输入姓名"
                  clearable
                  size="large"
                  prefix-icon="UserFilled">
                </el-input>
              </el-form-item>

              <el-form-item prop="mail">
                <el-input
                  v-model="addForm.mail"
                  placeholder="请输入邮箱"
                  clearable
                  size="large"
                  prefix-icon="Message">
                </el-input>
              </el-form-item>

              <el-form-item prop="phone">
                <el-input
                  v-model="addForm.phone"
                  placeholder="请输入手机号"
                  clearable
                  size="large"
                  prefix-icon="Iphone">
                </el-input>
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                  v-model="addForm.password"
                  type="password"
                  clearable
                  placeholder="请输入密码"
                  show-password
                  size="large"
                  prefix-icon="Lock">
                </el-input>
              </el-form-item>

              <el-button
                :loading="loading"
                type="primary"
                size="large"
                class="login-btn"
                @click="addUser(loginFormRef2)">
                注册
              </el-button>

              <div class="switch-text">
                已有账号？<span class="link-text" @click="changeLogin">立即登录</span>
              </div>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { setToken, setUsername, getUsername } from '@/core/auth.js'
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()
const API = proxy.$API
const loginFormRef1 = ref()
const loginFormRef2 = ref()
const router = useRouter()
const loginForm = reactive({
  username: 'admin',
  password: 'admin123456',
})
const addForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  mail: ''
})

const addFormRule = reactive({
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      pattern: /^1[3|5|7|8|9]\d{9}$/,
      message: '请输入正确的手机号',
      trigger: 'blur'
    },
    { min: 11, max: 11, message: '手机号必须是11位', trigger: 'blur' }
  ],
  username: [{ required: true, message: '请输入您的真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 15, message: '密码长度请在八位以上', trigger: 'blur' }
  ],
  mail: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    {
      pattern: /^([a-zA-Z]|[0-9])(\w|\-)+@[a-zA-Z0-9]+\.([a-zA-Z]{2,4})$/,
      message: '请输入正确的邮箱号',
      trigger: 'blur'
    }
  ],
  realNamee: [
    { required: true, message: '请输姓名', trigger: 'blur' },
  ]
})
const loginFormRule = reactive({
  username: [{ required: true, message: '请输入您的真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 15, message: '密码长度请在八位以上', trigger: 'blur' }
  ],
})
// 注册
const addUser = (formEl) => {
  if (!formEl) return
  formEl.validate(async (valid) => {
    if (valid) {
      // 检测用户名是否已经存在
      const res1 = await API.user.hasUsername({ username: addForm.username })
      if (res1.data.success !== false) {
        // 注册
        const res2 = await API.user.addUser(addForm)
        // console.log(res2)
        if (res2.data.success === false) {
          ElMessage.warning(res2.data.message)
        } else {
          const res3 = await API.user.login({ username: addForm.username, password: addForm.password })
          const token = res3?.data?.data?.token
          // 将username和token保存到cookies中和localStorage中
          if (token) {
            setToken(token)
            setUsername(addForm.username)
            localStorage.setItem('token', token)
            localStorage.setItem('username', addForm.username)
          }
          ElMessage.success('注册登录成功！')
          router.push('/home')
        }
      } else {
        ElMessage.warning('用户名已存在！')
      }
    } else {
      return false
    }
  })

}

// 登录
const login = (formEl) => {
  if (!formEl) return
  formEl.validate(async (valid) => {
    if (valid) {
      const res1 = await API.user.login(loginForm)
      if (res1.data.code === '0') {
        const token = res1?.data?.data?.token
        // 将username和token保存到cookies中和localStorage中
        if (token) {
          setToken(token)
          setUsername(loginForm.username)
          localStorage.setItem('token', token)
          localStorage.setItem('username', loginForm.username)
        }
        ElMessage.success('登录成功！')
        router.push('/home')
      } else if (res1.data.message === '用户已登录') {
        // 如果已经登录了，判断一下浏览器保存的登录信息是不是再次登录的信息，如果是就正常登录
        const cookiesUsername = getUsername()
        if (cookiesUsername === loginForm.username) {
          ElMessage.success('登录成功！')
          router.push('/home')
        } else {
          ElMessage.warning('用户已在别处登录，请勿重复登录！')
        }
      } else if (res1.data.message === '用户不存在') {
        ElMessage.error('请输入正确的账号密码!')
      }
    } else {
      return false
    }
  })


}

const loading = ref(false)
// 是否记住密码
const checked = ref(true)

// 展示登录还是展示注册
const isLogin = ref(true)
const changeLogin = () => {
  isLogin.value = !isLogin.value
}
</script>

<style lang="less" scoped>
/* 引入字体 */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

.login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #F7F7F2; /* Zed Light Theme Base: 暖调灰白 */
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  
  /* 灵魂细节：添加极细微的噪点纹理，提升质感 */
  &::before {
    content: "";
    position: absolute;
    top: 0; 
    left: 0;
    width: 100%; 
    height: 100%;
    opacity: 0.03; /* 极低透明度 */
    pointer-events: none;
    z-index: 1;
    background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
  }
}

// 1. 背景装饰：Zed 风格 - 极简网格 + 柔和中心光
.background-blur {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;

  /* 微型像素网格 - 模拟工程蓝图 */
  background-image: 
    linear-gradient(rgba(17, 24, 39, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(17, 24, 39, 0.03) 1px, transparent 1px);
  background-size: 40px 40px; /* 稍微大一点的网格，更显大气 */

  // 唯一的中心光源 - Ghostly Indigo (幽灵靛蓝)
  // 不再使用两个对角线的彩色球，而是用一个巨大的、极淡的中心漫射光
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 120vw; /* 超大尺寸，覆盖全屏 */
    height: 120vh;
    background: radial-gradient(circle, rgba(99, 102, 241, 0.08) 0%, rgba(247, 247, 242, 0) 60%);
    z-index: -1;
    pointer-events: none;
  }
}

.content-wrapper {
  position: relative;
  z-index: 10;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.title {
  font-size: 32px; /* Zed 风格倾向于更克制、更小的标题 */
  font-weight: 600; /* Medium 而不是 Bold */
  color: #171717; /* Neutral 900 */
  margin-bottom: 32px;
  letter-spacing: -0.02em;
  text-align: center;
  animation: fadeInDown 0.6s cubic-bezier(0.16, 1, 0.3, 1); /* 更利落的动画曲线 */
}

@keyframes fadeInDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-container {
  width: 100%;
  padding: 0 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

// Zed 风格卡片：极致平整，极细边框
.login-box {
  width: 400px; 
  padding: 40px 36px;
  background: rgba(255, 255, 255, 0.85); /* 稍微实一点 */
  backdrop-filter: blur(16px);       /* 稍微降低模糊，追求清晰 */
  -webkit-backdrop-filter: blur(16px);
  border-radius: 12px;               /* 稍微减小圆角，更严谨 */
  
  /* 关键：Zed 风格的边框是非常细且淡的 */
  border: 1px solid rgba(0, 0, 0, 0.06);
  
  /* 关键：几乎不可见的阴影，只有在 hover 时才显现 */
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.02),
    0 0 0 1px rgba(0, 0, 0, 0.02); 
  
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 
      0 8px 16px rgba(0, 0, 0, 0.04),
      0 0 0 1px rgba(0, 0, 0, 0.04);
  }
}

.form-section {
  h2 {
    font-size: 24px;
    font-weight: 700;
    color: #0f172a; /* Slate 900 */
    text-align: center;
    margin-bottom: 8px;
    letter-spacing: -0.01em;
  }

  .subtitle {
    font-size: 14px;
    color: #64748b; /* Slate 500 */
    text-align: center;
    margin-bottom: 32px;
  }
}

// 深度定制 Element Plus 样式
:deep(.el-form-item) {
  margin-bottom: 20px;

  .el-input__wrapper {
    background: #ffffff; /* 纯白背景，干净 */
    border-radius: 6px; /* 稍微减小圆角，更硬朗一点 */
    box-shadow: none;
    border: 1px solid #cbd5e1; /* Slate 300 */
    padding: 4px 11px;
    transition: all 0.2s ease;

    &:hover {
      border-color: #94a3b8; /* Slate 400 */
    }

    &.is-focus {
      border-color: #2563eb; /* Blue 600 - 纯正的 Tech Blue */
      box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1); /* 更细、更锐利的 Ring */
    }
  }

  .el-input__inner {
    color: #0f172a;
    font-size: 14px;
    font-weight: 500;
  }

  /* 图标颜色 */
  .el-input__prefix-inner {
    color: #94a3b8; /* Slate 400 */
  }
  
  .el-input__wrapper.is-focus .el-input__prefix-inner {
    color: #2563eb;
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  :deep(.el-checkbox) {
    .el-checkbox__label {
      color: #475569; /* Slate 600 */
      font-size: 13px;
    }

    .el-checkbox__inner {
      border-radius: 4px;
      border-color: #cbd5e1;
    }

    &.is-checked .el-checkbox__inner {
      background-color: #2563eb; /* Blue 600 */
      border-color: #2563eb;
    }
  }
}

// 按钮风格 - Geek Style
.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 6px; /* 稍微减小圆角 */
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.01em;
  background-color: #0f172a; /* Slate 900 */
  border: 1px solid transparent;
  color: #ffffff;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
  margin-top: 8px;

  &:hover {
    background-color: #1e293b; 
    transform: translateY(-1px);
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  }

  &:active {
    transform: translateY(0);
    background-color: #020617;
  }
  
  &.is-loading {
    background-color: #334155;
  }
}

.switch-text {
  text-align: center;
  margin-top: 24px;
  color: #64748b;
  font-size: 13px;

  .link-text {
    color: #2563eb; /* Blue 600 */
    font-weight: 600;
    cursor: pointer;
    margin-left: 4px;
    transition: all 0.2s;

    &:hover {
      color: #1d4ed8; /* Blue 700 */
      text-decoration: underline;
    }
  }
}

// 移除默认留白
:deep(.el-form-item__content) {
  margin-left: 0 !important;
}
</style>
