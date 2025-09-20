import axios from 'axios';
import { ElMessage } from 'element-plus'
import {useTokenStore} from '@/utils/stores/token.js'
import router from '@/router/index.js';
import { ENVI_PROD_PROTAL_SERVEER_ADDRESS, ENVI_DEV_PROTAL_SERVEER_ADDRESS } from '@/utils/globalCfg';

//全局环境变量
const ENVIRONMENT = import.meta.env;
console.log(ENVIRONMENT.MODE);

//门户服务端URL
const URL = ENVIRONMENT.PROD/*是否生产环境*/ 
            ? 
            ENVI_PROD_PROTAL_SERVEER_ADDRESS  //阿里云
            : 
            ENVI_DEV_PROTAL_SERVEER_ADDRESS; //localhost

//封装默认的请求实例
const axiosInstance =axios.create({
    baseURL: URL,
    timeout: 3000,
    headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' }
});


//请求拦截器
axiosInstance.interceptors.request.use(
    (config)=>{
        const tokenStore = useTokenStore();
        if(tokenStore.token){
            config.headers.Authorization = tokenStore.token;
        }
        return config;
    },
    (err)=>{
        console.error("请求发送失败.");
        Promise.reject(err);
    }
);

//添加响应拦截器
axiosInstance.interceptors.response.use(
    result=>{
        return result.data;
    },
    //当返回状态非2**开头时进入失败状态
    err=>{
        //未登录状态码
        if(err.response.status === 401){
            ElMessage.warning("请先登录");
            router.push('/');
        }else{
            console.error("Server反馈异常.");
            ElMessage.warning("Server Error");  
        }

        return Promise.reject(err);
    }
);

export default axiosInstance;