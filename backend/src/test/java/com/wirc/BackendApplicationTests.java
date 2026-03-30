package com.wirc;

import com.wirc.model.AppUser;
import com.wirc.model.ChatCommand;
import com.wirc.service.MessageService;
import com.wirc.service.RoomService;
import com.wirc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/wirc_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "wirc.state-file=target/test-state/chat-state.json"
})
class BackendApplicationTests {
    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;


    @BeforeEach
    void clearStateFile() throws Exception {
        Files.deleteIfExists(Path.of("target/test-state/chat-state.json"));
    }

    @Test
    void contextLoads() {
        assertThat(roomService.rooms("Ana")).hasSize(2);
    }

    @Test
    void sendsMessageUsingDisplayNameByPersistingCanonicalUsername() {
        messageService.sendMessage(new ChatCommand("room-equipa", "Ana", "Ana", "Olá websocket", false));

        assertThat(messageService.messagesByRoom("room-equipa", "Ana"))
                .extracting(message -> message.user() + ":" + message.message())
                .contains("ana:Olá websocket");
    }

    @Test
    void createsUserWithGeneratedUsername() {
        AppUser created = userService.createUser("Novo Utilizador", "1234");

        assertThat(created.username()).isEqualTo("novo-utilizador");
        assertThat(userService.signIn("Novo Utilizador", "1234").displayName()).isEqualTo("Novo Utilizador");
    }

    @Test
    void blocksReadingRoomsFromNonMembers() {
        assertThatThrownBy(() -> messageService.messagesByRoom("room-equipa", "Carla"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Só pode ver conteúdo das salas às quais pertence");
    }
}
