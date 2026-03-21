package com.wirc.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor
public class AppUserEntity {
    @Id
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;
}
