<<<<<<< HEAD
<<<<<<< HEAD
import {ref} from 'vue'
import {defineStore} from 'pinia'
=======
import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUserUsingPost } from '@/api/userContoller.ts'
>>>>>>> 53246a2 (frontend user module)
=======
import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUserUsingPost } from '@/api/userContoller.ts'
>>>>>>> 53246a2681b42fe1be0ba2bcde470c3e0d7905e6

/**
 * 存储登录用户信息的状态
 */
export const useLoginUserStore = defineStore('loginUser', () => {
<<<<<<< HEAD
<<<<<<< HEAD
  const loginUser = ref<any>({
    userName: '未登录',
  })

  async function fetchLoginUser() {
    // todo 由于后端还没提供接口，暂时注释
    // const res = await getCurrentUser();
    // if (res.data.code === 0 && res.data.data) {
    //   loginUser.value = res.data.data;
    // }
    // 测试用户登录，3 秒后自动登录
    setTimeout(() => {
      loginUser.value = {userName: '测试用户', id: 1}
    }, 3000)
=======
=======
>>>>>>> 53246a2681b42fe1be0ba2bcde470c3e0d7905e6
  const loginUser = ref<API.LoginUserVO>({
    userName: '未登录',
  })

  /**
   * 远程获取登录信息
   */
  async function fetchLoginUser() {
    const res = await getLoginUserUsingPost()
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data
    }
    // 测试用户登录，3 秒后自动登录
    // setTimeout(() => {
    //   loginUser.value = {userName: '测试用户', id: 1}
    // }, 3000)
<<<<<<< HEAD
>>>>>>> 53246a2 (frontend user module)
=======
>>>>>>> 53246a2681b42fe1be0ba2bcde470c3e0d7905e6
  }

  /**
   * 设置登录用户
   * @param newLoginUser
   */
  function setLoginUser(newLoginUser: any) {
    loginUser.value = newLoginUser
  }

  // 返回
<<<<<<< HEAD
<<<<<<< HEAD
  return {loginUser, fetchLoginUser, setLoginUser}
=======
  return { loginUser, fetchLoginUser, setLoginUser }
>>>>>>> 53246a2 (frontend user module)
=======
  return { loginUser, fetchLoginUser, setLoginUser }
>>>>>>> 53246a2681b42fe1be0ba2bcde470c3e0d7905e6
})
