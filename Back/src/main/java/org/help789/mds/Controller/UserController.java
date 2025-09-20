package org.help789.mds.Controller;


import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.help789.mds.Entity.Vo.RegisterReq;
import org.help789.mds.Service.UserService;
import org.help789.mds.Utils.pojo.Result;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    @Resource
    private UserService userService;

    // 账号密码登录（form-urlencoded）
    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<String> loginForm(
            @RequestParam String account,
            @RequestParam String password,
            @RequestParam(required = false, defaultValue = "true") Boolean remember
    ) {
        return userService.loginBySecretWithPassword(account, password);
    }

    // 邮箱验证码登录（form-urlencoded）
    @PostMapping(
            value = "/login-email",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<String> loginEmailForm(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam(required = false, defaultValue = "true") Boolean remember
    ) {
        return null;
    }

    // 发送验证码（form-urlencoded）
    @PostMapping(
            value = "/send-email-code",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<Void> sendCodeForm(@RequestParam String email) {
        return null;
    }

    // 注册（form-urlencoded）— 用 @ModelAttribute 绑定到 VO
    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<String> registerForm(@Valid RegisterReq req) { // 等价 @ModelAttribute RegisterReq req
        return userService.register(req); // 你前面已实现，返回 Result<String>
    }
}
