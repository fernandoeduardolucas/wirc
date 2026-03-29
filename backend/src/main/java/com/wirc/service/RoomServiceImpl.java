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

    private final ChatStateRegistry chatStateRegistry;
    private final DatabaseChatStateStore chatStateStore;
    private final ChatRoomRepository chatRoomRepository;
    private final UserServiceImpl userApplication;

    public RoomServiceImpl(
            ChatStateRegistry chatStateRegistry,
            DatabaseChatStateStore chatStateStore,
            ChatRoomRepository chatRoomRepository,
            UserServiceImpl userApplication) {
        this.chatStateRegistry = chatStateRegistry;
        this.chatStateStore = chatStateStore;
        this.chatRoomRepository = chatRoomRepository;
        this.userApplication = userApplication;
    }

    @Override
    public List<ChatRoom> rooms(String activeUser) {
        String canonicalUser = userApplication.requireCanonicalUser(activeUser);
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
        return toChatRoom(room, userApplication.requireCanonicalUser(activeUser));
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
        String canonicalUser = userApplication.requireCanonicalUser(activeUser);
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
        AppUserEntity actor = userApplication.resolveExistingUser(activeUser);
        String roomName = requireText(name, "Nome da sala obrigatório.");
        LinkedHashMap<String, String> canonicalParticipants = resolveCanonicalParticipants(actor, participants);

        String roomId = nextRoomId(roomName);
        ChatRoomEntity roomEntity = new ChatRoomEntity(roomId, roomName);
        addMembersToEntity(roomEntity, canonicalParticipants.values());
        chatRoomRepository.save(roomEntity);

        RoomSession roomSession = new RoomSession(roomId, roomName, new com.wirc.state.FocusedRoomState(), new ArrayList<>(canonicalParticipants.values()));
        chatStateRegistry.rooms().put(roomId, roomSession);
        persistState();
        return toChatRoom(roomSession, actor.getUsername());
    }

    @Override
    @Transactional
    public ChatRoom addMemberToRoom(String roomId, String member, String activeUser) {
        RoomSession room = chatStateRegistry.requireRoom(roomId);
        AppUserEntity actor = userApplication.resolveExistingUser(activeUser);
        ensureUserCanManageRoom(room, actor);

        AppUserEntity memberEntity = userApplication.resolveExistingUser(member);
        room.participants().add(memberEntity.getUsername());

        ChatRoomEntity roomEntity = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Sala não encontrada: " + roomId));
        roomEntity.addMember(memberEntity);
        chatRoomRepository.save(roomEntity);
        persistState();
        return toChatRoom(room, actor.getUsername());
    }

    public RoomSession requireAccessibleRoom(String roomId, String activeUser) {
        RoomSession room = chatStateRegistry.requireRoom(roomId);
        String canonicalUser = userApplication.requireCanonicalUser(activeUser);
        if (!room.participants().contains(canonicalUser)) {
            throw new IllegalArgumentException("Só pode ver conteúdo das salas às quais pertence.");
        }
        return room;
    }

    private ChatRoom toChatRoom(RoomSession room, String activeUser) {
        Map<String, AppUserEntity> usersByUsername = userApplication.loadUsersByUsername();
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
            throw new IllegalArgumentException("Só utilizadores da sala podem adicionar novos utilizadores.");
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
                .map(userApplication::resolveExistingUser)
                .forEach(participant -> canonicalParticipants.put(participant.getUsername(), participant.getUsername()));
        return canonicalParticipants;
    }

    private void addMembersToEntity(ChatRoomEntity roomEntity, Iterable<String> usernames) {
        for (String username : usernames) {
            roomEntity.addMember(userApplication.resolveExistingUser(username));
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
