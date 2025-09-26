package org.help789.mds.Service.ServiceImpl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.help789.mds.Entity.User;
import org.help789.mds.Entity.Vo.RegisterReq;
import org.help789.mds.Service.EmailCodeService;
import org.help789.mds.Service.JWTservice;
import org.help789.mds.Service.MailSenderService;
import org.help789.mds.Service.UserService;
import org.help789.mds.Utils.ThreadLocalUtil;
import org.help789.mds.Utils.pojo.Result;
import org.help789.mds.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final Set<String> ROLE_WHITELIST = Set.of(
            "administrators", "researchers", "analysts", "auditors"
    );
    @Resource
    private UserRepository userRepository;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JWTservice jwtservice;

    @Resource
    private EmailCodeService emailCodeService;
    @Resource
    private MailSenderService mailSenderService;

    @Override
    public Result<String> register(RegisterReq req) {
        // 1) 业务校验
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            return Result.failed("passwords do not match");
        }
        if (!ROLE_WHITELIST.contains(req.getRole())) {
            return Result.failed("invalid role");
        }
        if (userRepository.existsByAccount(req.getAccount())) {
            return Result.failed("account already exists");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            return Result.failed("email already exists");
        }

        // 2) 保存用户（密码用 BCrypt 存 hash）
        String hash = passwordEncoder.encode(req.getPassword());
        User user = User.builder()
                .realName(req.getAccount())         // 没有单独 realName 的话先用 account 填；有的话改成 req.getRealName()
                .account(req.getAccount())
                .passwordHash(hash)
                .email(req.getEmail())
                .role(req.getRole())
                .build();

        userRepository.save(user);

        // 3) 返回成功
        return Result.success("registered successfully",user.getRealName());
    }

    @Override
    public Result<String> loginByEmail(String email, String code) {
        // 统一邮箱校验（只允许 qq / foxmail 可选）
        if (email == null || email.isBlank()
                || !(email.endsWith("@qq.com") || email.endsWith("@foxmail.com"))) {
            return Result.failed("仅支持 QQ/Foxmail 邮箱");
        }
        if (code == null || code.isBlank()) {
            return Result.failed("验证码不能为空");
        }

        boolean ok = emailCodeService.verify(email, code);
        if (!ok) return Result.failed("验证码或邮箱错误");

        // 查或建用户（注意你的表结构字段 NOT NULL）
        var opt = userRepository.findByEmail(email);
        User user = opt.orElseGet(() -> {
            // account/realName/passwordHash 均为 NOT NULL，给默认值
            String account = email.split("@")[0];
            String randomPwd = java.util.UUID.randomUUID().toString(); // 占位
            String hash = passwordEncoder.encode(randomPwd);

            User u = User.builder()
                    .realName(account)
                    .account(account)
                    .passwordHash(hash)
                    .email(email)
                    .role("user")
                    .build();
            return userRepository.save(u);
        });

        // 生成 JWT
        try {
            Map<String, Object> claims = Map.of(
                    "userId", user.getId(),
                    "account", user.getAccount(),
                    "role", user.getRole(),
                    "realName", user.getRealName(),
                    "email", user.getEmail()
            );
            String token = jwtservice.getJWTToken(claims);
            return Result.success("登录成功", token);
        } catch (Exception e) {
            return Result.failed("签发令牌失败");
        }
    }

    @Override
    public Result<String> loginBySecretWithPassword(String account, String password) {
        // 1) 基本校验
        if (account == null || account.isBlank() || password == null || password.isBlank()) {
            return Result.failed("account 或 password 不能为空");
        }

        // 2) 查用户
        Optional<User> opt = userRepository.findByAccount(account);

        // 3) 为避免侧信道，做一次 dummy bcrypt 校验
        final String DUMMY_BCRYPT = "$2a$10$7EqJtq98hPqEX7fNZaFWoOe.VN3S8S8QeG/5hOaVY1GZ7HcH0bK2K"; // 明文 "password"
        String hash = opt.map(User::getPasswordHash).orElse(DUMMY_BCRYPT);

        boolean ok = passwordEncoder.matches(password, hash);
        if (opt.isEmpty() || !ok) {
            return Result.failed("账号或密码错误");
        }

        User user = opt.get();

        // 4) 生成 JWT（失败则兜底）
        String token;
        try {
            Map<String, Object> claims = Map.of(
                    "userId", user.getId(),
                    "account", user.getAccount(),
                    "role", user.getRole(),
                    "realName", user.getRealName()
            );
            token = jwtservice.getJWTToken(claims);
        } catch (Exception e) {
            // 极端情况下给个兜底（仅开发调试；生产建议直接返回失败）
            token = UUID.randomUUID().toString();
        }

        // 5) 如需记录登录态/审计日志，可在此处处理
        Map<String, Object> claims = ThreadLocalUtil.createUserInfoClaim(user.getId(), user.getAccount());
        ThreadLocalUtil.setClaim(claims);
        System.out.println("userid: " + ThreadLocalUtil.getCurrentUserId());
        return Result.success("登录成功", token);
    }


}
