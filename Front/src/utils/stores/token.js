import {ref} from 'vue'
import { defineStore } from 'pinia'

//存储本系统token
export const useTokenStore = defineStore('userToken', ()=>{

    const token = ref('');

    const setToken = (newToken)=>{
        token.value=newToken;
    }

    const removeToken = ()=>{
        token.value = '';
    }

    return {
        token, setToken, removeToken
    }
}, {
    persist: true //持久化存储token
});

//存储门户token
export const useProtalTokenStore = defineStore('protalToken', ()=>{

    const token = ref('');

    const setToken = (newToken)=>{
        token.value=newToken;
    }

    const removeToken = ()=>{
        token.value = '';
    }

    return {
        token, setToken, removeToken
    }
}, {
    persist: true //持久化存储token
});