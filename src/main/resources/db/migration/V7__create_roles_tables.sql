CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE user_roles (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Default user role'),
    ('ROLE_ADMIN', 'Administrator role');
