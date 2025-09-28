package org.help789.mds.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.help789.mds.Entity.User;
import org.help789.mds.Entity.Vo.RegisterReq;
import org.help789.mds.Service.EmailCodeService;
import org.help789.mds.Service.JWTservice;
import org.help789.mds.Service.MailSenderService;
import org.help789.mds.Service.ServiceImpl.UserServiceImpl;
import org.help789.mds.Utils.pojo.Result;
import org.help789.mds.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTservice jwtservice;

    @Mock
    private EmailCodeService emailCodeService;

    @Mock
    private MailSenderService mailSenderService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // 初始化Mockito注解
    }

    @Test
    void testRegister() {
        // 模拟 RegisterReq 对象
        RegisterReq req = new RegisterReq();
        req.setAccount("testUser");
        req.setPassword("password123");
        req.setConfirmPassword("password123");
        req.setEmail("test@example.com");
        req.setRole("administrators");

        // 模拟 userRepository 的行为
        when(userRepository.existsByAccount("testUser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        // 模拟成功的保存操作
        when(userRepository.save(any(User.class))).thenReturn(new User());

        // 执行注册操作并验证结果
        Result<String> result = userService.register(req);

        // 验证返回的结果和行为
        assertEquals("registered successfully", result.getMsg());
        verify(userRepository, times(1)).save(any(User.class));  // 确保 save 被调用
    }


    @Test
    void testLoginBySecretWithPasswordInvalid() {
        String account = "testUser";
        String password = "wrongPassword";

        // 模拟 userRepository 查找用户
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setAccount(account);
        mockUser.setPasswordHash("hashedPassword");
        when(userRepository.findByAccount(account)).thenReturn(java.util.Optional.of(mockUser));

        // 模拟密码匹配失败
        when(passwordEncoder.matches(password, "hashedPassword")).thenReturn(false);

        // 执行登录操作
        Result<String> result = userService.loginBySecretWithPassword(account, password);

        // 验证返回的结果
        assertEquals("账号或密码错误", result.getMsg());
    }

    @Test
    void testSendEmailCode() {
        String email = "2421411703@qq.com";

        // 模拟邮箱存在
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // 模拟验证码生成和发送
        when(emailCodeService.generateAndStore(email)).thenReturn("123456");
        doNothing().when(mailSenderService).sendLoginCode(email, "123456");

        // 模拟 HttpServletRequest
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // 执行发送验证码操作
        Result<Void> result = userService.sendCodeForm(email, request);

        // 验证返回的结果
        assertEquals("验证码已发送", result.getMsg());
    }
}
