package org.help789.mds.Controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.help789.mds.Entity.Vo.RegisterReq;
import org.help789.mds.Service.EmailCodeService;
import org.help789.mds.Service.MailSenderService;
import org.help789.mds.Service.UserService;
import org.help789.mds.Utils.ThreadLocalUtil;
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

    /** Account & password login (form-urlencoded) */
    @LogAction(
            value = "user:login",
            detail = "Account/password login: account=#{#account}, remember=#{#remember?:false}"
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
        // ⚠️ Do NOT log the password
        return userService.loginBySecretWithPassword(account, password);
    }

    /** Send verification code (form-urlencoded) */
    @LogAction(
            value = "user:send-code",
            detail = "Send email verification code: email=#{#email}, IP=#{#req?.getHeader('X-Forwarded-For')?:#req?.getHeader('X-Real-IP')?:#req?.getRemoteAddr()}"
    )
    @PostMapping(
            value = "/send-email-code",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<Void> sendCodeForm(@RequestParam @Email String email, HttpServletRequest req) {
        if (!email.endsWith("@qq.com") && !email.endsWith("@foxmail.com")) {
            return Result.failed("Only QQ/Foxmail email is supported");
        }
        if (!userRepository.existsByEmail(email)) {
            return Result.failed("Email is not registered");
        }
        String ip = realIp(req);
        try {
            emailCodeService.checkRateLimit(email, ip);
            String code = emailCodeService.generateAndStore(email);
            mailSenderService.sendLoginCode(email, code);
            return Result.success("Verification code sent", (Void) null);
        } catch (IllegalStateException tooMany) {
            return Result.failed(tooMany.getMessage());
        } catch (Exception e) {
            return Result.failed("Failed to send, please try again later");
        }
    }

    @LogAction(
            value = "user:login-email",
            detail = "Email code login: email=#{#email}, code length=#{#code?.length()?:0}"
    )
    @PostMapping(
            value = "/login-email",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<String> loginEmailForm(@RequestParam @Email String email,
                                         @RequestParam String code) {
        // ⚠️ Do NOT log the raw code, only its length
        return userService.loginByEmail(email, code);
    }

    // Registration (form-urlencoded) — bind to VO with @ModelAttribute
    @LogAction(
            value = "user:register",
            detail = "User registration: account=#{#req?.account?:'-'}, email=#{#req?.email?:'-'}"
    )
    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<String> registerForm(@Valid RegisterReq req) {
        // ⚠️ Do NOT log sensitive fields like passwords
        return userService.register(req);
    }

    private String realIp(HttpServletRequest req){
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String rip = req.getHeader("X-Real-IP");
        return (rip != null && !rip.isBlank()) ? rip : req.getRemoteAddr();
    }

    @GetMapping(
            value = "/getUserId",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Result<String> getUserId() {
        System.out.println(userService.getUserId());
        return userService.getUserId(); // Ensure this returns Result<String>
    }
}
