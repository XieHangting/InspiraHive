/// <reference types="vite/client" />
declare module '*.vue' {
  import { DefineComponent } from 'vue';
// eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/ban-types
  const component: DefineComponent<{}, {}, any>;
  export default component;   // 表示所有 .vue 文件都将默认导出一个组件，供 TypeScript 在其他地方正确地 import 和使用这些文件。
}
