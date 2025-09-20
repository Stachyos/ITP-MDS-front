/**
 * 随机生成指定长度字符串
 * @param {} length 
 * @returns 
 */
export function generateRandomString(length) {
    const characters = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    let randomID = '';

    for (let i = 0; i < length; i++) {
        const randomIndex = Math.floor(Math.random() * characters.length);
        randomID += characters.charAt(randomIndex);
    }

    return randomID;
}

/**
 * 判断密码是否满足强度要求
 * @param {} pwd 
 * @returns 
 */
export function checkPwd(pwd) {
    let num = false;
    let word = false;
    for (let i = 0; i < pwd.length; i++){
        let c = pwd.charCodeAt(i)
        if (c >= 48&& c <= 57){
            //数字
            num = true;
        }else if (c >= 65 && c <= 90){
            //大写字母
            word = true;
        }else if (c = 97 && c <= 122){
            //小写字母
            word = true;
        }else {
            
        }
    }
    return num && word;
}