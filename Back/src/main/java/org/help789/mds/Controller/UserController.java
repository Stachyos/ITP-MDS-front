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
import org.help789.mds.logging.LogAction;
import org.help789.mds.repository.UserRepository;
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
    @Resource
    private UserRepository userRepository;

    /** 账号密码登录（form-urlencoded） */
    @LogAction(
            value = "user:login",
            detail = "账号密码登录：account=#{#account}，记住登录=#{#remember?:false}"
    )
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
        // ⚠️ 不记录 password
        return userService.loginBySecretWithPassword(account, password);
    }

    /** 发送验证码（form-urlencoded） */
    @LogAction(
            value = "user:send-code",
            detail = "发送邮箱验证码：email=#{#email}，IP=#{#req?.getHeader('X-Forwarded-For')?:#req?.getHeader('X-Real-IP')?:#req?.getRemoteAddr()}"
    )
    @PostMapping(
            value = "/send-email-code",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<Void> sendCodeForm(@RequestParam @Email String email, HttpServletRequest req) {
        if (!email.endsWith("@qq.com") && !email.endsWith("@foxmail.com")) {
            return Result.failed("仅支持 QQ/Foxmail 邮箱");
        }
        if (!userRepository.existsByEmail(email)) {
            return Result.failed("邮箱未注册");
        }
        String ip = realIp(req);
        try {
            emailCodeService.checkRateLimit(email, ip);
            String code = emailCodeService.generateAndStore(email);
            mailSenderService.sendLoginCode(email, code);
            return Result.success("验证码已发送", (Void) null);
        } catch (IllegalStateException tooMany) {
            return Result.failed(tooMany.getMessage());
        } catch (Exception e) {
            return Result.failed("发送失败，请稍后再试");
        }
    }

    @LogAction(
            value = "user:login-email",
            detail = "邮箱验证码登录：email=#{#email}，验证码长度=#{#code?.length()?:0}"
    )
    @PostMapping(
            value = "/login-email",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<String> loginEmailForm(@RequestParam @Email String email,
                                         @RequestParam String code) {
        // ⚠️ 不记录明文 code，只记录长度
        return userService.loginByEmail(email, code);
    }

    // 注册（form-urlencoded）— 用 @ModelAttribute 绑定到 VO
    @LogAction(
            value = "user:register",
            detail = "用户注册：account=#{#req?.account?:'-'}，email=#{#req?.email?:'-'}"
    )
    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<String> registerForm(@Valid RegisterReq req) {
        // ⚠️ 不记录密码等敏感字段
        return userService.register(req);
    }

    private String realIp(HttpServletRequest req){
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String rip = req.getHeader("X-Real-IP");
        return (rip != null && !rip.isBlank()) ? rip : req.getRemoteAddr();
    }
}
