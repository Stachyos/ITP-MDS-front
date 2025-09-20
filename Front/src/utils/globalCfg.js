//本系统服务端地址
export const ENVI_DEV_SERVER_ADDRESS = 'http://127.0.0.1:7886'; //localhost
export const ENVI_PROD_SERVER_ADDRESS = 'http://47.121.119.235:7886';  //阿里云

//综合服务门户前端地址
export const ENVI_DEV_PROTAL_ADDRESS = 'http://120.76.202.164:7788'; //localhost
export const ENVI_PROD_PROTAL_ADDRESS = 'http://47.121.119.235:80'; //阿里云

//综合服务门户服务端地址
export const ENVI_DEV_PROTAL_SERVEER_ADDRESS = 'http://192.168.6.251:7786'; //localhost
export const ENVI_PROD_PROTAL_SERVEER_ADDRESS = 'http://47.121.119.235:7786'; //阿里云


//此类数据改成从字典表拿

//样品类型
export const SAMPLE_TYPE_ENUM = ['C岩石', 'H化探', 'S水', '纯加工'];

//检测项目
export const EXPERIMENT_CLASS_ENUM = ['Ag', 'As', 'Sb', 'Hg', 'Au', 'Cu', 'Zn'];

//测试仪器
export const INSTRUMENT_ENUM = [
    'CCD',
    'AFS',
    'MS',
    'XRF',
    'ISE',
    'OES',
    'CS',
    'COL',
    '元素分析仪'];

//实验任务状态 需要与服务器保持一直
export const EXPERIMENT_STATUS_ENUM = [
    '未处理', //0
    '已发起', //1
    '已完成' //2
]
