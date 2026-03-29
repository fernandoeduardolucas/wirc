package com.wirc.service;

import com.wirc.entity.AppUserEntity;
import com.wirc.model.AppUser;
import com.wirc.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final AppUserRepository appUserRepository;

    public UserServiceImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
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
        String normalizedDisplayName = validateDisplayName(displayName);
        String normalizedPassword = validatePassword(password);
        String username = nextUsername(normalizedDisplayName);

        AppUserEntity userToCreate = new AppUserEntity(username, normalizedDisplayName, normalizedPassword);
        AppUserEntity createdUser = appUserRepository.save(userToCreate);
        return toAppUser(createdUser);
    }

    @Override
    public String requireCanonicalUser(String activeUser) {
        return resolveCanonicalUsername(activeUser)
                .orElseThrow(() -> new IllegalArgumentException("Autentique-se para aceder às salas."));
    }

    @Override
    public AppUserEntity resolveExistingUser(String user) {
        return appUserRepository.findByUsernameIgnoreCase(user)
                .or(() -> appUserRepository.findByDisplayNameIgnoreCase(user))
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado: " + user));
    }

    @Override
    public Optional<String> resolveCanonicalUsername(String user) {
        if (user == null || user.isBlank()) {
            return Optional.empty();
        }
        return appUserRepository.findByUsernameIgnoreCase(user)
                .map(AppUserEntity::getUsername)
                .or(() -> appUserRepository.findByDisplayNameIgnoreCase(user)
                        .map(AppUserEntity::getUsername));
    }

    @Override
    public Map<String, AppUserEntity> loadUsersByUsername() {
        return appUserRepository.findAllByOrderByDisplayNameAsc().stream()
                .collect(Collectors.toMap(
                        AppUserEntity::getUsername,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private AppUserEntity authenticate(String user, String password) {
        AppUserEntity appUser = resolveExistingUser(user);
        String normalizedPassword = password == null ? "" : password.trim();
        if (!appUser.getPassword().equals(normalizedPassword)) {
            throw new IllegalArgumentException("Credenciais inválidas.");
        }
        return appUser;
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String validateDisplayName(String displayName) {
        String normalizedDisplayName = requireText(displayName, "Nome do utilizador obrigatório.");
        ensureDisplayNameAvailable(normalizedDisplayName);
        return normalizedDisplayName;
    }

    private String validatePassword(String password) {
        return requireText(password, "Password obrigatória.");
    }

    private AppUser toAppUser(AppUserEntity userEntity) {
        return new AppUser(userEntity.getUsername(), userEntity.getDisplayName());
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
}
