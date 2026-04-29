package com.wastecoder.picpay.user;

import com.wastecoder.picpay.user.domain.enums.UserType;
import com.wastecoder.picpay.user.domain.model.User;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Object Mother for {@link User} test fixtures.
 *
 * Typical usage:
 *   User user = UserObjectMother.aCommonUser()
 *           .withEmail("alice@example.com")
 *           .build();
 *
 * Defaults populate every required field, so each test only overrides what is
 * relevant for its scenario.
 */
public class UserObjectMother {

    public static final String FULL_NAME_DEFAULT = "John Doe Smith";
    public static final String DOCUMENT_DEFAULT = "123.456.789-00";
    public static final String EMAIL_DEFAULT = "john.doe@example.com";
    public static final String PASSWORD_DEFAULT = "secret-password";
    public static final UserType TYPE_DEFAULT = UserType.COMMON;
    public static final BigDecimal BALANCE_DEFAULT = BigDecimal.ZERO;
    public static final UUID ID_DEFAULT = null;

    private String fullName = FULL_NAME_DEFAULT;
    private String document = DOCUMENT_DEFAULT;
    private String email = EMAIL_DEFAULT;
    private String password = PASSWORD_DEFAULT;
    private UserType type = TYPE_DEFAULT;
    private BigDecimal balance = BALANCE_DEFAULT;
    private UUID id = ID_DEFAULT;

    private UserObjectMother() {
    }

    // =========================================================
    // Entry points
    // =========================================================

    public static UserObjectMother aUser() {
        return new UserObjectMother();
    }

    public static UserObjectMother aCommonUser() {
        return aUser().withType(UserType.COMMON);
    }

    public static UserObjectMother aMerchantUser() {
        return aUser().withType(UserType.MERCHANT);
    }

    // =========================================================
    // Fluent setters
    // =========================================================

    public UserObjectMother withFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public UserObjectMother withDocument(String document) {
        this.document = document;
        return this;
    }

    public UserObjectMother withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserObjectMother withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserObjectMother withType(UserType type) {
        this.type = type;
        return this;
    }

    public UserObjectMother withBalance(BigDecimal balance) {
        this.balance = balance;
        return this;
    }

    public UserObjectMother withId(UUID id) {
        this.id = id;
        return this;
    }

    // =========================================================
    // Terminal
    // =========================================================

    public User build() {
        return new User(fullName, document, email, password, type, balance, id);
    }
}
