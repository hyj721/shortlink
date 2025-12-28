<!--
 * @Date: 2022-10-26 13:46:41
 * @LastEditTime: 2022-10-26 14:00:56
 * @Description: 列表空状态
-->
<script setup>
defineProps({
  text: {
    type: String,
    default: '暂无数据，请点击创建短链按钮创建数据'
  }
})
</script>

<template>
  <div class="empty">
    <img src="@/assets/png/无数据.png" alt="" class="empty_img" />
    <p>{{ text }}</p>
    <slot></slot>
  </div>
</template>

<style lang="scss" scoped>
.empty {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  font-weight: 400;
  color: #64748b; /* Slate 500 */
  height: 440px;

  &_img {
    width: 200px;
    margin-bottom: 16px;
  }
}
</style>
