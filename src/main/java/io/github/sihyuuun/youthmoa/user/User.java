package io.github.sihyuuun.youthmoa.user;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 191)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 10)
    private String zipcode;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String addressDetail;

    @Column
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private UserGender gender;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_interest", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "interest", length = 50)
    private Set<String> interests = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    private Center center;

    @Column(length = 50)
    private String centerScope;

    @Builder
    private User(String email, String password, String name, String phone,
                 String zipcode, String address, String addressDetail,
                 LocalDate birthDate, UserGender gender, Set<String> interests,
                 UserRole role, Center center, String centerScope) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.zipcode = zipcode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.birthDate = birthDate;
        this.gender = gender;
        this.interests = interests != null ? interests : new HashSet<>();
        this.role = role != null ? role : UserRole.USER;
        this.center = center;
        this.centerScope = centerScope;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateProfile(String name, String phone, String zipcode,
                              String address, String addressDetail,
                              LocalDate birthDate, Set<String> interests) {
        this.name = name;
        this.phone = phone;
        this.zipcode = zipcode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.birthDate = birthDate;
        this.interests = interests != null ? interests : new HashSet<>();
    }

    public void assignRole(UserRole role, Center center, String centerScope) {
        this.role = role;
        this.center = center;
        this.centerScope = centerScope;
    }
}
