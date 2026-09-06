<template>
  <router-view v-slot="{ Component, route }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>

<script setup>
// App 根组件
</script>

<style>
/* 全局基础样式由 global.css 接管 */
</style>
