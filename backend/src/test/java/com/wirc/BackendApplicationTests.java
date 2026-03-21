package com.wirc;

import com.wirc.service.ChatApplicationFacade;
import com.wirc.model.ChatCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/wirc_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "wirc.state-file=target/test-state/chat-state.json"
})
class BackendApplicationTests {
    @Autowired
    private ChatApplicationFacade chatApplicationFacade;

    @BeforeEach
    void clearStateFile() throws Exception {
        Files.deleteIfExists(Path.of("target/test-state/chat-state.json"));
    }

    @Test
    void contextLoads() {
        assertThat(chatApplicationFacade.rooms()).hasSize(3);
    }

    @Test
    void sendsMessageUsingDisplayNameByPersistingCanonicalUsername() {
        chatApplicationFacade.sendMessage(new ChatCommand("room-equipa", "Ana", "Olá websocket", false));

        assertThat(chatApplicationFacade.messagesByRoom("room-equipa"))
                .extracting(message -> message.user() + ":" + message.message())
                .contains("ana:Olá websocket");
    }
}
