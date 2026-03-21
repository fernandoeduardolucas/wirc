INSERT INTO app_user (username, display_name, password) VALUES
    ('ana', 'Ana', 'Ana'),
    ('bruno', 'Bruno', 'Bruno'),
    ('carla', 'Carla', 'Carla'),
    ('diogo', 'Diogo', 'Diogo'),
    ('eva', 'Eva', 'Eva');

INSERT INTO chat_room (id, name) VALUES
    ('room-ana-bruno', 'Ana & Bruno'),
    ('room-equipa', 'Equipa Projeto'),
    ('room-estudo', 'Grupo de Estudo');

INSERT INTO chat_room_member (room_id, username) VALUES
    ('room-ana-bruno', 'ana'),
    ('room-ana-bruno', 'bruno'),
    ('room-equipa', 'ana'),
    ('room-equipa', 'bruno'),
    ('room-equipa', 'carla'),
    ('room-equipa', 'diogo'),
    ('room-estudo', 'carla'),
    ('room-estudo', 'diogo'),
    ('room-estudo', 'eva');
