package org.ktb.sideproject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "refresh-token.hash-secret=test-refresh-token-hash-secret-which-is-long-enough-for-context-tests",
        "spring.datasource.username=root",
        "spring.datasource.password=1234",
        "jwt.secret=Tm5KRFFUWTBTMWsyTkhWck5qVXlPRFkyWlVrMk5IVnJOMGxMY3pkS1YwVTNTalpSTjB4RGJ6ZE1iVEEzV1U5Qk4xbDVUVGRhVjFrPQ=="
})
class SideProjectApplicationTests {

    @Test
    void contextLoads() {
    }

}
