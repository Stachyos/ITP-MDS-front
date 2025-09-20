import {ref} from 'vue'
import { defineStore } from 'pinia'

//存储本系统token
export const useLocalCfgStore = defineStore('localCfg', ()=>{

    const fullPageSize = ref(10);
    const setFullPageSize = (value) => {
        fullPageSize.value = value;
      }
    
    return { fullPageSize, setFullPageSize }
}, {
    persist: true //持久化存储token
});