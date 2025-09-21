package org.help789.mds.Controller;


import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.help789.mds.Entity.Vo.RegisterReq;
import org.help789.mds.Service.EmailCodeService;
import org.help789.mds.Service.MailSenderService;
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
    @Resource
    private EmailCodeService emailCodeService;
    @Resource
    private MailSenderService mailSenderService;


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

    // 发送验证码（form-urlencoded）
    @PostMapping(
            value = "/send-email-code",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<Void> sendCodeForm(@RequestParam @Email String email, HttpServletRequest req) {
        if (!email.endsWith("@qq.com") && !email.endsWith("@foxmail.com")) {
            return Result.failed("仅支持 QQ/Foxmail 邮箱"); // 泛型会推断为 Void
        }
        String ip = realIp(req);
        try {
            emailCodeService.checkRateLimit(email, ip);
            String code = emailCodeService.generateAndStore(email);
            mailSenderService.sendLoginCode(email, code);
            // 关键：第二个参数传 null，使其成为 Result<Void>
            return Result.success("验证码已发送", (Void) null);
        } catch (IllegalStateException tooMany) {
            return Result.failed(tooMany.getMessage());
        } catch (Exception e) {
            return Result.failed("发送失败，请稍后再试");
        }
    }


    @PostMapping(
            value = "/login-email",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<String> loginEmailForm(@RequestParam @Email String email,
                                         @RequestParam String code) {
        return userService.loginByEmail(email, code); // UserService 返回 Result<String>
    }


    private String realIp(HttpServletRequest req){
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String rip = req.getHeader("X-Real-IP");
        return (rip != null && !rip.isBlank()) ? rip : req.getRemoteAddr();
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
