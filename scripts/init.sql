-- H2 schema. Existing tables and data are preserved on repeated initialization.
CREATE TABLE IF NOT EXISTS app_users (
    login VARCHAR(128) PRIMARY KEY,
    password_salt VARCHAR(128) NOT NULL,
    password_hash CHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS resources (
    path VARCHAR(1024) PRIMARY KEY,
    maximum_volume INT NOT NULL CHECK (maximum_volume >= 0)
);

CREATE TABLE IF NOT EXISTS permissions (
    login VARCHAR(128) NOT NULL,
    resource_path VARCHAR(1024) NOT NULL,
    action VARCHAR(16) NOT NULL CHECK (action IN ('READ', 'WRITE', 'EXECUTE')),
    PRIMARY KEY (login, resource_path, action),
    FOREIGN KEY (login) REFERENCES app_users(login) ON DELETE CASCADE,
    FOREIGN KEY (resource_path) REFERENCES resources(path) ON DELETE CASCADE
);
