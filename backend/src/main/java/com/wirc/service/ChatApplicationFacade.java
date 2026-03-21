package com.wirc.service;

import com.wirc.bootstrap.DatabaseChatRoomLoader;
import com.wirc.entity.AppUserEntity;
import com.wirc.entity.ChatRoomEntity;
import com.wirc.model.*;
import com.wirc.persistence.DatabaseChatStateStore;
import com.wirc.repository.AppUserRepository;
import com.wirc.repository.ChatRoomRepository;
import com.wirc.validation.MessageLengthValidationHandler;
import com.wirc.validation.MessageValidationHandler;
import com.wirc.validation.ParticipantValidationHandler;
import com.wirc.validation.RequiredFieldValidationHandler;
import com.wirc.websocket.WebSocketNotificationGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatApplicationFacade {
    private final Map<String, RoomSession> rooms = new ConcurrentHashMap<>();
    private final WebSocketNotificationGateway notificationGateway;
    private final MessageValidationHandler validationChain;
    private final DatabaseChatStateStore chatStateStore;
    private final AppUserRepository appUserRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ChatApplicationFacade(
            ChatRoomFactory roomFactory,
            DatabaseChatRoomLoader databaseChatRoomLoader,
            WebSocketNotificationGateway notificationGateway,
            DatabaseChatStateStore chatStateStore,
            AppUserRepository appUserRepository,
            ChatRoomRepository chatRoomRepository) {
        this.notificationGateway = notificationGateway;
        this.chatStateStore = chatStateStore;
        this.appUserRepository = appUserRepository;
        this.chatRoomRepository = chatRoomRepository;
        loadRooms(roomFactory, databaseChatRoomLoader);
        this.validationChain = buildValidationChain();
    }

    private void loadRooms(ChatRoomFactory roomFactory, DatabaseChatRoomLoader databaseChatRoomLoader) {
        databaseChatRoomLoader.loadRooms()
                .forEach(snapshot -> rooms.put(snapshot.id(), roomFactory.createFromSnapshot(snapshot)));
    }

    private MessageValidationHandler buildValidationChain() {
        MessageValidationHandler required = new RequiredFieldValidationHandler();
        required
                .linkWith(new ParticipantValidationHandler(rooms))
                .linkWith(new MessageLengthValidationHandler());
        return required;
    }

    public List<AppUser> users() {
        return loadUsersByUsername().values().stream()
                .map(user -> new AppUser(user.getUsername(), user.getDisplayName(), user.getDisplayName()))
                .toList();
    }

    public List<ChatRoom> rooms(String activeUser) {
        String canonicalUser = resolveCanonicalUsername(activeUser).orElse("");
        return rooms.values().stream()
                .sorted(Comparator.comparing(RoomSession::name))
                .map(room -> toChatRoom(room, canonicalUser))
                .toList();
    }

    public List<ChatMessage> messagesByRoom(String roomId) {
        return new ArrayList<>(requireRoom(roomId).messages());
    }

    public List<ChatMessage> searchMessages(String term) {
        String normalized = term.toLowerCase(Locale.ROOT);
        return rooms.values().stream()
                .flatMap(room -> room.messages().stream())
                .filter(message -> message.message().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }

    public ChatMessage sendMessage(ChatCommand command) {
        String canonicalUsername = resolveCanonicalUsername(command.user())
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado: " + command.user()));
        ChatCommand validatedCommand = new ChatCommand(
                command.roomId(),
                canonicalUsername,
                command.message(),
                command.focusedRoom());
        validationChain.validate(validatedCommand);
        RoomSession room = requireRoom(command.roomId());
        boolean highlighted = command.message().toLowerCase(Locale.ROOT).contains("graphql")
                || command.message().toLowerCase(Locale.ROOT).contains("websocket");

        ChatMessage chatMessage = new ChatMessage(
                UUID.randomUUID().toString(),
                room.id(),
                canonicalUsername,
                command.message(),
                Instant.now(),
                highlighted);

        room.messages().add(chatMessage);
        room.state().onMessageSent(room, command.focusedRoom());
        persistState();

        notificationGateway.broadcast(new ChatNotification(
                room.id(),
                room.name(),
                command.message(),
                canonicalUsername,
                "NEW_MESSAGE",
                chatMessage.id(),
                chatMessage.sentAt(),
                chatMessage.highlighted()
        ));

        return chatMessage;
    }

    public ChatRoom focusRoom(String roomId, String activeUser) {
        RoomSession room = requireRoom(roomId);
        room.state().onRoomFocused(room);
        persistState();
        return toChatRoom(room, resolveCanonicalUsername(activeUser).orElse(""));
    }

    public RoomStats roomStats(String roomId) {
        RoomSession room = requireRoom(roomId);
        SequencedMap<String, Long> perUser = room.messages().stream()
                .collect(Collectors.groupingBy(ChatMessage::user, LinkedHashMap::new, Collectors.counting()));

        String busiestUser = perUser.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");

        long highlighted = room.messages().stream().filter(ChatMessage::highlighted).count();
        return new RoomStats(room.id(), room.name(), room.messages().size(), Math.toIntExact(highlighted), busiestUser);
    }

    public List<UserMessageCount> topUsers() {
        return rooms.values().stream()
                .flatMap(room -> room.messages().stream())
                .collect(Collectors.groupingBy(ChatMessage::user, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> new UserMessageCount(entry.getKey(), Math.toIntExact(entry.getValue())))
                .toList();
    }

    @Transactional
    public ChatRoom createRoom(String name, String activeUser, List<String> participants) {
        AppUserEntity actor = authenticate(activeUser, activeUser);
        String roomName = requireText(name, "Nome do canal obrigatório.");
        List<String> requestedParticipants = normalizeParticipantList(participants);
        LinkedHashMap<String, String> canonicalParticipants = new LinkedHashMap<>();
        canonicalParticipants.put(actor.getUsername(), actor.getUsername());
        requestedParticipants.stream()
                .map(this::resolveExistingUser)
                .forEach(participant -> canonicalParticipants.put(participant.getUsername(), participant.getUsername()));

        String roomId = nextRoomId(roomName);
        ChatRoomEntity roomEntity = new ChatRoomEntity(roomId, roomName);
        canonicalParticipants.values().forEach(username -> roomEntity.addMember(resolveExistingUser(username)));
        chatRoomRepository.save(roomEntity);

        RoomSession roomSession = new RoomSession(roomId, roomName, new com.wirc.state.FocusedRoomState(), new ArrayList<>(canonicalParticipants.values()));
        rooms.put(roomId, roomSession);
        persistState();
        return toChatRoom(roomSession, actor.getUsername());
    }

    @Transactional
    public ChatRoom addMemberToRoom(String roomId, String member, String activeUser) {
        RoomSession room = requireRoom(roomId);
        AppUserEntity actor = authenticate(activeUser, activeUser);
        if (!room.participants().contains(actor.getUsername())) {
            throw new IllegalArgumentException("Só membros do canal podem adicionar novos membros.");
        }

        AppUserEntity memberEntity = resolveExistingUser(member);
        room.participants().add(memberEntity.getUsername());

        ChatRoomEntity roomEntity = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Sala não encontrada: " + roomId));
        roomEntity.addMember(memberEntity);
        chatRoomRepository.save(roomEntity);
        persistState();
        return toChatRoom(room, actor.getUsername());
    }

    private AppUserEntity authenticate(String user, String password) {
        AppUserEntity appUser = resolveExistingUser(user);
        if (!appUser.getDisplayName().equals(password) && !appUser.getUsername().equalsIgnoreCase(password)) {
            throw new IllegalArgumentException("Credenciais inválidas. Use user/password iguais ao utilizador.");
        }
        return appUser;
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

    private String nextRoomId(String roomName) {
        String normalized = Normalizer.normalize(roomName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String base = normalized.isBlank() ? "canal" : normalized;
        String candidate = "room-" + base;
        int suffix = 2;
        while (rooms.containsKey(candidate)) {
            candidate = "room-" + base + "-" + suffix++;
        }
        return candidate;
    }

    private AppUserEntity resolveExistingUser(String user) {
        return appUserRepository.findByUsernameIgnoreCase(user)
                .or(() -> appUserRepository.findByDisplayNameIgnoreCase(user))
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado: " + user));
    }

    private Optional<String> resolveCanonicalUsername(String user) {
        if (user == null || user.isBlank()) {
            return Optional.empty();
        }
        return appUserRepository.findByUsernameIgnoreCase(user)
                .map(AppUserEntity::getUsername)
                .or(() -> appUserRepository.findByDisplayNameIgnoreCase(user)
                        .map(AppUserEntity::getUsername));
    }

    private ChatRoom toChatRoom(RoomSession room, String activeUser) {
        Map<String, AppUserEntity> usersByUsername = loadUsersByUsername();
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

    private void persistState() {
        chatStateStore.save(rooms.values().stream()
                .sorted(Comparator.comparing(RoomSession::name))
                .map(RoomSession::snapshot)
                .toList());
    }

    private RoomSession requireRoom(String roomId) {
        RoomSession room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Sala não encontrada: " + roomId);
        }
        return room;
    }

    private Map<String, AppUserEntity> loadUsersByUsername() {
        return appUserRepository.findAllByOrderByDisplayNameAsc().stream()
                .collect(Collectors.toMap(
                        AppUserEntity::getUsername,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }
}
