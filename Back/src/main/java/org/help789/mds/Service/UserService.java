package org.help789.mds.Service;

import org.help789.mds.Utils.pojo.Result;

public interface UserService {

    public Result<String> loginBySecretWithPassword(String account, String password);

    public Result<String> loginByEmail(String email);

}
