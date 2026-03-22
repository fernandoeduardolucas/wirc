package com.wirc.service;

import com.wirc.bootstrap.DatabaseChatRoomLoader;
import com.wirc.common.DatabaseChatStateStore;
import com.wirc.common.RoomSession;
import com.wirc.entity.AppUserEntity;
import com.wirc.entity.ChatRoomEntity;
import com.wirc.factory.ChatRoomFactory;
import com.wirc.gateway.WebSocketNotificationGateway;
import com.wirc.model.*;
import com.wirc.repository.AppUserRepository;
import com.wirc.repository.ChatRoomRepository;
import com.wirc.validation.MessageLengthValidationHandler;
import com.wirc.validation.MessageValidationHandler;
import com.wirc.validation.ParticipantValidationHandler;
import com.wirc.validation.RequiredFieldValidationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatApplicationImpl implements ChatApplication {
    private static final List<String> HIGHLIGHT_KEYWORDS = List.of("graphql", "websocket");

    private final Map<String, RoomSession> rooms = new ConcurrentHashMap<>();
    private final WebSocketNotificationGateway notificationGateway;
    private final MessageValidationHandler validationChain;
    private final DatabaseChatStateStore chatStateStore;
    private final AppUserRepository appUserRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ChatApplicationImpl(
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

    @Override
    public List<AppUser> users() {
        return loadUsersByUsername().values().stream()
                .map(user -> new AppUser(user.getUsername(), user.getDisplayName()))
                .toList();
    }

    @Override
    public AppUser signIn(String user, String password) {
        AppUserEntity authenticatedUser = authenticate(user, password);
        return new AppUser(authenticatedUser.getUsername(), authenticatedUser.getDisplayName());
    }

    @Override
    @Transactional
    public AppUser createUser(String displayName, String password) {
        String normalizedDisplayName = requireText(displayName, "Nome do utilizador obrigatório.");
        String normalizedPassword = requireText(password, "Password obrigatória.");
        ensureDisplayNameAvailable(normalizedDisplayName);

        String username = nextUsername(normalizedDisplayName);
        AppUserEntity createdUser = appUserRepository.save(new AppUserEntity(username, normalizedDisplayName, normalizedPassword));
        return new AppUser(createdUser.getUsername(), createdUser.getDisplayName());
    }

    @Override
    public List<ChatRoom> rooms(String activeUser) {
        String canonicalUser = requireCanonicalUser(activeUser);
        return rooms.values().stream()
                .filter(room -> room.participants().contains(canonicalUser))
                .sorted(Comparator.comparing(RoomSession::name))
                .map(room -> toChatRoom(room, canonicalUser))
                .toList();
    }

    @Override
    public List<ChatMessage> messagesByRoom(String roomId, String activeUser) {
        RoomSession room = requireAccessibleRoom(roomId, activeUser);
        return new ArrayList<>(room.messages());
    }

    @Override
    public List<ChatMessage> searchMessages(String term, String activeUser) {
        String canonicalUser = requireCanonicalUser(activeUser);
        String normalized = term.toLowerCase(Locale.ROOT);
        return rooms.values().stream()
                .filter(room -> room.participants().contains(canonicalUser))
                .flatMap(room -> room.messages().stream())
                .filter(message -> message.message().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }

    @Override
    public ChatMessage sendMessage(ChatCommand command) {
        ChatCommand validatedCommand = validateCommand(command);
        RoomSession room = requireRoom(validatedCommand.roomId());
        log.info("Processando mensagem entre utilizadores: roomId={}, roomName={}, user={}, focusedRoom={}, messageLength={}",
                room.id(), room.name(), validatedCommand.user(), validatedCommand.focusedRoom(), validatedCommand.message().length());
        ChatMessage chatMessage = createChatMessage(room, validatedCommand.user(), validatedCommand.message());

        room.messages().add(chatMessage);
        room.state().onMessageSent(room, validatedCommand.focusedRoom());
        persistState();
        broadcastNewMessage(room, chatMessage);
        log.info("Mensagem persistida e difundida: roomId={}, messageId={}, user={}, highlighted={}",
                room.id(), chatMessage.id(), chatMessage.user(), chatMessage.highlighted());

        return chatMessage;
    }

    @Override
    public ChatRoom focusRoom(String roomId, String activeUser) {
        RoomSession room = requireAccessibleRoom(roomId, activeUser);
        room.state().onRoomFocused(room);
        persistState();
        return toChatRoom(room, requireCanonicalUser(activeUser));
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
        String canonicalUser = requireCanonicalUser(activeUser);
        return rooms.values().stream()
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
        AppUserEntity actor = resolveExistingUser(activeUser);
        String roomName = requireText(name, "Nome da sala obrigatório.");
        LinkedHashMap<String, String> canonicalParticipants = resolveCanonicalParticipants(actor, participants);

        String roomId = nextRoomId(roomName);
        ChatRoomEntity roomEntity = new ChatRoomEntity(roomId, roomName);
        addMembersToEntity(roomEntity, canonicalParticipants.values());
        chatRoomRepository.save(roomEntity);

        RoomSession roomSession = new RoomSession(roomId, roomName, new com.wirc.state.FocusedRoomState(), new ArrayList<>(canonicalParticipants.values()));
        rooms.put(roomId, roomSession);
        persistState();
        return toChatRoom(roomSession, actor.getUsername());
    }

    @Override
    @Transactional
    public ChatRoom addMemberToRoom(String roomId, String member, String activeUser) {
        RoomSession room = requireRoom(roomId);
        AppUserEntity actor = resolveExistingUser(activeUser);
        ensureUserCanManageRoom(room, actor);

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
        String normalizedPassword = password == null ? "" : password.trim();
        if (!appUser.getPassword().equals(normalizedPassword)) {
            throw new IllegalArgumentException("Credenciais inválidas.");
        }
        return appUser;
    }

    private ChatCommand validateCommand(ChatCommand command) {
        String canonicalUsername = resolveCanonicalUsername(command.user())
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado: " + command.user()));
        ChatCommand validatedCommand = new ChatCommand(
                command.roomId(),
                canonicalUsername,
                command.message(),
                command.focusedRoom());
        validationChain.validate(validatedCommand);
        return validatedCommand;
    }

    private ChatMessage createChatMessage(RoomSession room, String canonicalUsername, String message) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                room.id(),
                canonicalUsername,
                message,
                Instant.now(),
                isHighlighted(message));
    }

    private boolean isHighlighted(String message) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return HIGHLIGHT_KEYWORDS.stream().anyMatch(normalizedMessage::contains);
    }

    private void broadcastNewMessage(RoomSession room, ChatMessage chatMessage) {
        notificationGateway.broadcast(new ChatNotification(
                room.id(),
                room.name(),
                chatMessage.message(),
                chatMessage.user(),
                "NEW_MESSAGE",
                chatMessage.id(),
                chatMessage.sentAt(),
                chatMessage.highlighted()
        ));
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
                .map(this::resolveExistingUser)
                .forEach(participant -> canonicalParticipants.put(participant.getUsername(), participant.getUsername()));
        return canonicalParticipants;
    }

    private void addMembersToEntity(ChatRoomEntity roomEntity, Iterable<String> usernames) {
        for (String username : usernames) {
            roomEntity.addMember(resolveExistingUser(username));
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
        while (rooms.containsKey(candidate)) {
            candidate = "room-" + base + "-" + suffix++;
        }
        return candidate;
    }

    private String nextUsername(String displayName) {
        String normalized = Normalizer.normalize(displayName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String base = normalized.isBlank() ? "utilizador" : normalized;
        String candidate = base;
        int suffix = 2;
        while (appUserRepository.findByUsernameIgnoreCase(candidate).isPresent()) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private void ensureDisplayNameAvailable(String displayName) {
        if (appUserRepository.findByDisplayNameIgnoreCase(displayName).isPresent()) {
            throw new IllegalArgumentException("Já existe um utilizador com esse nome.");
        }
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

    private String requireCanonicalUser(String activeUser) {
        return resolveCanonicalUsername(activeUser)
                .orElseThrow(() -> new IllegalArgumentException("Autentique-se para aceder às salas."));
    }

    private RoomSession requireAccessibleRoom(String roomId, String activeUser) {
        RoomSession room = requireRoom(roomId);
        String canonicalUser = requireCanonicalUser(activeUser);
        if (!room.participants().contains(canonicalUser)) {
            throw new IllegalArgumentException("Só pode ver conteúdo das salas às quais pertence.");
        }
        return room;
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

    private void ensureUserCanManageRoom(RoomSession room, AppUserEntity actor) {
        if (!room.participants().contains(actor.getUsername())) {
            throw new IllegalArgumentException("Só utilizadores da sala podem adicionar novos utilizadores.");
        }
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
}
