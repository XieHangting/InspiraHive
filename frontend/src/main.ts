import {createApp} from 'vue'
import {createPinia} from 'pinia'

// @ts-ignore
import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
<<<<<<< HEAD
<<<<<<< HEAD
=======
import '@/access.ts'
>>>>>>> 53246a2 (frontend user module)
=======
import '@/access.ts'
>>>>>>> 53246a2681b42fe1be0ba2bcde470c3e0d7905e6

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)

app.mount('#app')


