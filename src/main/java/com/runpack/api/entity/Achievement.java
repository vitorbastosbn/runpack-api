package com.runpack.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "achievement")
@Getter @Setter @NoArgsConstructor
public class Achievement {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    private String iconUrl;
}
