CREATE TABLE app_user (
    username VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE chat_room (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(150) NOT NULL
);

CREATE TABLE chat_room_member (
    room_id VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    CONSTRAINT pk_chat_room_member PRIMARY KEY (room_id, username),
    CONSTRAINT fk_room_member_room FOREIGN KEY (room_id) REFERENCES chat_room (id),
    CONSTRAINT fk_room_member_user FOREIGN KEY (username) REFERENCES app_user (username)
);
