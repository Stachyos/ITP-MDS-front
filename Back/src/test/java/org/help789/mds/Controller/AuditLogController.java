package org.help789.mds.Controller;

import org.help789.mds.Entity.AuditLog;
import org.help789.mds.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditLogRepository repo;

    @InjectMocks
    private AuditLogController auditLogController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(auditLogController).build();
    }

    @Test
    void testList() throws Exception {
        // 准备数据
        AuditLog log1 = new AuditLog();
        log1.setId(1L);
        log1.setUserId(123L);
        log1.setAction("Login");

        AuditLog log2 = new AuditLog();
        log2.setId(2L);
        log2.setUserId(124L);
        log2.setAction("Logout");

        List<AuditLog> auditLogs = Arrays.asList(log1, log2);
        Page<AuditLog> page = new PageImpl<>(auditLogs, PageRequest.of(0, 10), auditLogs.size());

        // Mock：分页查询（无条件）
        when(repo.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/auditLog/list")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].userId").value(123))
                .andExpect(jsonPath("$.data.content[1].userId").value(124));

        verify(repo, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testListByUser() throws Exception {
        // 准备数据：同一 userId
        AuditLog log1 = new AuditLog();
        log1.setId(1L);
        log1.setUserId(123L);
        log1.setAction("Login");

        AuditLog log2 = new AuditLog();
        log2.setId(2L);
        log2.setUserId(123L);
        log2.setAction("Logout");

        List<AuditLog> auditLogs = Arrays.asList(log1, log2);
        Page<AuditLog> page = new PageImpl<>(auditLogs, PageRequest.of(0, 10), auditLogs.size());

        // Mock：使用 Specification 的分页查询
        when(repo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/auditLog/user/123")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].userId").value(123))
                .andExpect(jsonPath("$.data.content[1].userId").value(123));

        verify(repo, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetById() throws Exception {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setUserId(123L);
        log.setAction("Login");

        when(repo.findById(1L)).thenReturn(Optional.of(log));

        mockMvc.perform(get("/api/auditLog/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.userId").value(123))
                .andExpect(jsonPath("$.data.action").value("Login"));

        verify(repo, times(1)).findById(1L);
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auditLog/999")
                        .contentType(MediaType.APPLICATION_JSON))
                // 控制器当前是返回 Result.failed(...)，并未设置 404，所以这里应为 200
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("日志不存在"));

        verify(repo, times(1)).findById(999L);
    }
}
