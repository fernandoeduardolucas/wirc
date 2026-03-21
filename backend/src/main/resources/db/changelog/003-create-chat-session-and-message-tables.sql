CREATE TABLE room_session_state (
    room_id VARCHAR(100) PRIMARY KEY,
    state VARCHAR(50) NOT NULL,
    unread_messages BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_room_session_state_room FOREIGN KEY (room_id) REFERENCES chat_room (id)
);

CREATE TABLE chat_message (
    id VARCHAR(100) PRIMARY KEY,
    room_id VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    highlighted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES chat_room (id),
    CONSTRAINT fk_chat_message_user FOREIGN KEY (username) REFERENCES app_user (username)
);
