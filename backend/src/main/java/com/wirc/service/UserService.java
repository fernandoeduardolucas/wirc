package com.wirc.service;

import com.wirc.entity.AppUserEntity;
import com.wirc.model.AppUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserService {
    List<AppUser> users();

    AppUser signIn(String user, String password);

    AppUser createUser(String displayName, String password);

    String requireCanonicalUser(String activeUser);

    AppUserEntity resolveExistingUser(String user);

    Optional<String> resolveCanonicalUsername(String user);

    Map<String, AppUserEntity> loadUsersByUsername();
}
