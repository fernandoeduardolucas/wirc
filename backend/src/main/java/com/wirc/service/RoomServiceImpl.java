package com.wirc.service;

import com.wirc.common.DatabaseChatStateStore;
import com.wirc.common.RoomSession;
import com.wirc.entity.AppUserEntity;
import com.wirc.entity.ChatRoomEntity;
import com.wirc.model.ChatMessage;
import com.wirc.model.ChatRoom;
import com.wirc.model.RoomStats;
import com.wirc.model.UserMessageCount;
import com.wirc.repository.ChatRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    private static final String ROOM_NOT_FOUND = "Sala não encontrada: ";
    private static final String USER_NOT_IN_ROOM = "Só pode ver conteúdo das salas às quais pertence.";
    private static final String USER_CANNOT_MANAGE_ROOM = "Só utilizadores da sala podem adicionar novos utilizadores.";

    private final ChatStateRegistry chatStateRegistry;
    private final DatabaseChatStateStore chatStateStore;
    private final ChatRoomRepository chatRoomRepository;
    private final UserService userFacade;

    public RoomServiceImpl(
            ChatStateRegistry chatStateRegistry,
            DatabaseChatStateStore chatStateStore,
            ChatRoomRepository chatRoomRepository,
            UserService userFacade) {
        this.chatStateRegistry = chatStateRegistry;
        this.chatStateStore = chatStateStore;
        this.chatRoomRepository = chatRoomRepository;
        this.userFacade = userFacade;
    }

    @Override
    public List<ChatRoom> rooms(String activeUser) {
        String canonicalUser = userFacade.requireCanonicalUser(activeUser);
        return chatStateRegistry.rooms().values().stream()
                .filter(room -> room.participants().contains(canonicalUser))
                .sorted(Comparator.comparing(RoomSession::name))
                .map(room -> toChatRoom(room, canonicalUser))
                .toList();
    }

    @Override
    public ChatRoom focusRoom(String roomId, String activeUser) {
        RoomSession room = requireAccessibleRoom(roomId, activeUser);
        room.state().onRoomFocused(room);
        persistState();
        return toChatRoom(room, userFacade.requireCanonicalUser(activeUser));
    }

    @Override
    public RoomStats roomStats(String roomId, String activeUser) {
        RoomSession room = requireAccessibleRoom(roomId, activeUser);
        SequencedMap<String, Long> perUser = room.messages().stream()
                .collect(Collectors.groupingBy(ChatMessage::user, LinkedHashMap::new, Collectors.counting()));

        String busiestUser = perUser.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");

        long highlighted = room.messages().stream().filter(ChatMessage::highlighted).count();
        return new RoomStats(room.id(), room.name(), room.messages().size(), Math.toIntExact(highlighted), busiestUser);
    }

    @Override
    public List<UserMessageCount> topUsers(String activeUser) {
        String canonicalUser = userFacade.requireCanonicalUser(activeUser);
        return chatStateRegistry.rooms().values().stream()
                .filter(room -> room.participants().contains(canonicalUser))
                .flatMap(room -> room.messages().stream())
                .collect(Collectors.groupingBy(ChatMessage::user, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> new UserMessageCount(entry.getKey(), Math.toIntExact(entry.getValue())))
                .toList();
    }

    @Override
    @Transactional
    public ChatRoom createRoom(String name, String activeUser, List<String> participants) {
        AppUserEntity actor = userFacade.resolveExistingUser(activeUser);
        String roomName = requireText(name, "Nome da sala obrigatório.");
        LinkedHashMap<String, String> canonicalParticipants = resolveCanonicalParticipants(actor, participants);

        RoomSession roomSession = createAndStoreRoom(roomName, canonicalParticipants.values());
        persistState();
        return toChatRoom(roomSession, actor.getUsername());
    }

    @Override
    @Transactional
    public ChatRoom addMemberToRoom(String roomId, String member, String activeUser) {
        RoomSession room = chatStateRegistry.requireRoom(roomId);
        AppUserEntity actor = userFacade.resolveExistingUser(activeUser);
        ensureUserCanManageRoom(room, actor);

        AppUserEntity memberEntity = userFacade.resolveExistingUser(member);
        addParticipantIfMissing(room, memberEntity.getUsername());

        ChatRoomEntity roomEntity = requireRoomEntity(roomId);
        addMemberToEntity(roomEntity, memberEntity);
        persistState();
        return toChatRoom(room, actor.getUsername());
    }

    public RoomSession requireAccessibleRoom(String roomId, String activeUser) {
        RoomSession room = chatStateRegistry.requireRoom(roomId);
        String canonicalUser = userFacade.requireCanonicalUser(activeUser);
        if (!room.participants().contains(canonicalUser)) {
            throw new IllegalArgumentException(USER_NOT_IN_ROOM);
        }
        return room;
    }

    private ChatRoom toChatRoom(RoomSession room, String activeUser) {
        Map<String, AppUserEntity> usersByUsername = userFacade.loadUsersByUsername();
        return new ChatRoom(
                room.id(),
                room.name(),
                room.participants().stream()
                        .map(username -> usersByUsername.containsKey(username)
                                ? usersByUsername.get(username).getDisplayName()
                                : username)
                        .toList(),
                room.state().name(),
                Math.toIntExact(room.unreadMessages()),
                !activeUser.isBlank() && room.participants().contains(activeUser));
    }

    private void ensureUserCanManageRoom(RoomSession room, AppUserEntity actor) {
        if (!room.participants().contains(actor.getUsername())) {
            throw new IllegalArgumentException(USER_CANNOT_MANAGE_ROOM);
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private List<String> normalizeParticipantList(List<String> participants) {
        if (participants == null) {
            return List.of();
        }
        return participants.stream()
                .filter(participant -> participant != null && !participant.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private LinkedHashMap<String, String> resolveCanonicalParticipants(AppUserEntity actor, List<String> participants) {
        LinkedHashMap<String, String> canonicalParticipants = new LinkedHashMap<>();
        canonicalParticipants.put(actor.getUsername(), actor.getUsername());
        normalizeParticipantList(participants).stream()
                .map(userFacade::resolveExistingUser)
                .forEach(participant -> canonicalParticipants.put(participant.getUsername(), participant.getUsername()));
        return canonicalParticipants;
    }

    private RoomSession createAndStoreRoom(String roomName, Iterable<String> usernames) {
        String roomId = nextRoomId(roomName);
        List<String> participantList = toList(usernames);

        ChatRoomEntity roomEntity = new ChatRoomEntity(roomId, roomName);
        addMembersToEntity(roomEntity, participantList);
        chatRoomRepository.save(roomEntity);

        RoomSession roomSession = new RoomSession(
                roomId,
                roomName,
                new com.wirc.state.FocusedRoomState(),
                new ArrayList<>(participantList));
        chatStateRegistry.rooms().put(roomId, roomSession);
        return roomSession;
    }

    private List<String> toList(Iterable<String> usernames) {
        List<String> participantList = new ArrayList<>();
        usernames.forEach(participantList::add);
        return participantList;
    }

    private ChatRoomEntity requireRoomEntity(String roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException(ROOM_NOT_FOUND + roomId));
    }

    private void addParticipantIfMissing(RoomSession room, String username) {
        if (!room.participants().contains(username)) {
            room.participants().add(username);
        }
    }

    private void addMemberToEntity(ChatRoomEntity roomEntity, AppUserEntity memberEntity) {
        roomEntity.addMember(memberEntity);
        chatRoomRepository.save(roomEntity);
    }

    private void addMembersToEntity(ChatRoomEntity roomEntity, Iterable<String> usernames) {
        for (String username : usernames) {
            roomEntity.addMember(userFacade.resolveExistingUser(username));
        }
    }

    private String nextRoomId(String roomName) {
        String normalized = Normalizer.normalize(roomName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String base = normalized.isBlank() ? "sala" : normalized;
        String candidate = "room-" + base;
        int suffix = 2;
        while (chatStateRegistry.rooms().containsKey(candidate)) {
            candidate = "room-" + base + "-" + suffix++;
        }
        return candidate;
    }

    private void persistState() {
        chatStateStore.save(chatStateRegistry.sortedRooms().stream()
                .map(RoomSession::snapshot)
                .toList());
    }
}
