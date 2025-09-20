package org.help789.mds.Service.ServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.help789.mds.Service.UserService;
import org.help789.mds.Utils.pojo.Result;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {


    @Override
    public Result<String> loginBySecretWithPassword(String secret) {
        return null;
    }

    @Override
    public Result<String> loginByEmail(String username) {
        return null;
    }


}
