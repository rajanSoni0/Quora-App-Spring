package org.example.quoraproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Application user.
 *
 * Fixes from the original model:
 * - Removed the duplicate {@code private Long id} field which shadowed the
 *   inherited {@code @Id} from the base class (a latent mapping bug).
 * - Added unique constraints on username and email.
 * - Password is stored only as a BCrypt hash and is never serialized
 *   (entities are never exposed through controllers anymore).
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @jakarta.persistence.UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @jakarta.persistence.UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_tags",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> followedTags = new HashSet<>();
}
