package org.help789.mds.Service;

import org.help789.mds.Entity.Vo.RegisterReq;
import org.help789.mds.Utils.pojo.Result;

public interface UserService {

    public Result<String> loginBySecretWithPassword(String account, String password);


    Result<String> register(RegisterReq req);
    // UserService.java
    Result<String> loginByEmail(String email, String code);

}
