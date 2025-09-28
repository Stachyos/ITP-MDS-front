package org.help789.mds.Controller;

import org.help789.mds.Entity.Vo.HealthRecordShowVo;
import org.help789.mds.Service.HealthRecordShowService;
import org.help789.mds.Utils.pojo.ImportResult;
import org.help789.mds.Utils.pojo.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HealthRecordShowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HealthRecordShowService healthRecordShowService;

    @InjectMocks
    private HealthRecordShowController healthRecordShowController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(healthRecordShowController).build();
    }

    @Test
    void testGetAll() throws Exception {
        // Mock the service to return a list of HealthRecordShowVo
        HealthRecordShowVo record = new HealthRecordShowVo();
        record.setName("Test");
        record.setSex("Male");
        record.setAge(30);
        when(healthRecordShowService.listAll()).thenReturn(List.of(record));

        mockMvc.perform(get("/api/HealthRecordShow/getAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test"))
                .andExpect(jsonPath("$.data[0].sex").value("Male"))
                .andExpect(jsonPath("$.data[0].age").value(30));

        verify(healthRecordShowService, times(1)).listAll();
    }

    @Test
    void testCreate() throws Exception {
        HealthRecordShowVo record = new HealthRecordShowVo();
        record.setName("Test User");
        record.setSex("Male");
        record.setAge(25);

        // Mock the service to return the created record
        when(healthRecordShowService.create(any(HealthRecordShowVo.class))).thenReturn(record);

        mockMvc.perform(post("/api/HealthRecordShow/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test User\",\"sex\":\"Male\",\"age\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.data.sex").value("Male"))
                .andExpect(jsonPath("$.data.age").value(25));

        verify(healthRecordShowService, times(1)).create(any(HealthRecordShowVo.class));
    }

    @Test
    void testUpdate() throws Exception {
        HealthRecordShowVo record = new HealthRecordShowVo();
        record.setName("Updated User");
        record.setSex("Female");
        record.setAge(28);

        // Mock the service to return the updated record
        when(healthRecordShowService.update(eq(1L), any(HealthRecordShowVo.class))).thenReturn(record);

        mockMvc.perform(put("/api/HealthRecordShow/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated User\",\"sex\":\"Female\",\"age\":28}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated User"))
                .andExpect(jsonPath("$.data.sex").value("Female"))
                .andExpect(jsonPath("$.data.age").value(28));

        verify(healthRecordShowService, times(1)).update(eq(1L), any(HealthRecordShowVo.class));
    }

    @Test
    void testDelete() throws Exception {
        // Mock the service to delete the record
        doNothing().when(healthRecordShowService).delete(1L);

        mockMvc.perform(delete("/api/HealthRecordShow/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("deleted"));

        verify(healthRecordShowService, times(1)).delete(1L);
    }

    @Test
    void testDownloadTemplate() throws Exception {
        // Create a MockHttpServletResponse to capture the response
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Mock the service to write the template
        doNothing().when(healthRecordShowService).writeTemplate(response.getOutputStream());

        mockMvc.perform(get("/api/HealthRecordShow/downloadTemplate"))
                .andExpect(status().isOk());

        verify(healthRecordShowService, times(1)).writeTemplate(any());
    }

}
