-- Only missing seed rows are inserted. Existing passwords and limits are not overwritten.
SET AUTOCOMMIT FALSE;

INSERT INTO app_users (login, password_salt, password_hash)
SELECT * FROM (VALUES
    ('alice', 'alice-salt-2026', 'e91c00f48dd574a479152d06a37b919eff4669324e080aa7fc90de3a94e722b6'),
    ('bob', 'bob-salt-2026', '4316aa71d2a86d3572a169b043e7a1a18d2909b73f0d92756722f82d9e408a93')
) AS seed(login, password_salt, password_hash)
WHERE NOT EXISTS (SELECT 1 FROM app_users u WHERE u.login = seed.login);

INSERT INTO resources (path, maximum_volume)
SELECT * FROM (VALUES
    ('A', 100), ('A.B', 50), ('A.B.C', 20),
    ('A.A8B', 40), ('A.A8B.C', 30), ('A.A8B.C.f_d', 10)
) AS seed(path, maximum_volume)
WHERE NOT EXISTS (SELECT 1 FROM resources r WHERE r.path = seed.path);

INSERT INTO permissions (login, resource_path, action)
SELECT * FROM (VALUES
    ('alice', 'A', 'READ'),
    ('alice', 'A.B.C', 'WRITE'),
    ('alice', 'A.B.C', 'EXECUTE'),
    ('bob', 'A.A8B', 'READ'),
    ('bob', 'A.A8B', 'WRITE')
) AS seed(login, resource_path, action)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p
    WHERE p.login = seed.login AND p.resource_path = seed.resource_path AND p.action = seed.action
);
COMMIT;
