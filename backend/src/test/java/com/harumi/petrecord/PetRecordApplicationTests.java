package com.harumi.petrecord;

import com.harumi.petrecord.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PetRecordApplicationTests {

    @Test
    void contextLoads() {
    }

}
