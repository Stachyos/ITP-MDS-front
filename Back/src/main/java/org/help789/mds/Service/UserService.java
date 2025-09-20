package org.help789.mds.Service;

import org.help789.mds.Utils.pojo.Result;

public interface UserService {

    public Result<String> loginBySecretWithPassword(String secret);

    public Result<String> loginByEmail(String username);

}
