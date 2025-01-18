<template>
  <div id="app">
    <BasicLayout />
  </div>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
<<<<<<< HEAD
<<<<<<< HEAD
import { healthUsingGet } from '@/api/mainController.ts'
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";

const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()

healthUsingGet().then((res) => {
  console.log(res)
})
=======
=======
>>>>>>> 53246a2681b42fe1be0ba2bcde470c3e0d7905e6
// import { healthUsingGet } from '@/api/mainController.ts'
// import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";

// 改为在access.ts权限校验文件中获取
// const loginUserStore = useLoginUserStore()
// loginUserStore.fetchLoginUser()

// healthUsingGet().then((res) => {
//   console.log(res)
// })
<<<<<<< HEAD
>>>>>>> 53246a2 (frontend user module)
=======
>>>>>>> 53246a2681b42fe1be0ba2bcde470c3e0d7905e6
</script>

<style scoped></style>
