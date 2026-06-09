package com.example.WonkaoTalk;

import com.example.WonkaoTalk.config.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestContainerConfig.class)
class WonkaoTalkApplicationTests {

	@Test
	void contextLoads() {
	}

}
