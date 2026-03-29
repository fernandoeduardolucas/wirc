package com.wirc.service;

import com.wirc.model.AppUser;

import java.util.List;

public interface UserService {
    List<AppUser> users();

    AppUser signIn(String user, String password);

    AppUser createUser(String displayName, String password);
}
