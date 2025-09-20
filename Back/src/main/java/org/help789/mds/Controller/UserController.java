package org.help789.mds.Controller;



import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.help789.mds.Service.UserService;
import org.help789.mds.Utils.pojo.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController

@RequestMapping("/user")
@Validated
public class UserController {

    @Resource
    private UserService userService;
    @PostMapping("/loginBySecret")
    public Result<String> loginBySecret(@RequestParam @NotBlank(message = "秘钥secret必填") @Parameter(description = "一次性秘钥") String secret){
        Result<String> result = userService.loginBySecret(secret);

        return result;
    }

}
