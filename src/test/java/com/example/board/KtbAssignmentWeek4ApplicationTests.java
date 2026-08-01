package com.example.board;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KtbAssignmentWeek4ApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Spring 애플리케이션 컨텍스트가 정상적으로 로드된다.")
	void contextLoads() {
	}

	@Test
	@DisplayName("Health Check는 인증 없이 애플리케이션 준비 상태를 반환한다.")
	void healthCheckReturnsUpWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

}
