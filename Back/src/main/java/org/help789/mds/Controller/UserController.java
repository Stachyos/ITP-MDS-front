package org.help789.mds.Controller;



import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.help789.mds.Entity.Vo.LoginReq;
import org.help789.mds.Service.UserService;
import org.help789.mds.Utils.pojo.Result;
import org.hibernate.annotations.Parameter;
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
    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public Result<String> login(@Validated @RequestBody LoginReq req) {

        return userService.loginBySecretWithPassword(req.getAccount(), req.getPassword());
    }

}
