CREATE TABLE IF NOT EXISTS third_party_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    principal_type VARCHAR(20) NOT NULL,
    principal_id INT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    platform VARCHAR(30) NOT NULL,
    openid VARCHAR(128) NOT NULL,
    unionid VARCHAR(128) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_third_party_identity (principal_type, provider, platform, openid),
    KEY idx_third_party_principal (principal_type, principal_id),
    KEY idx_third_party_unionid (provider, unionid)
);

INSERT INTO third_party_account (
    principal_type,
    principal_id,
    provider,
    platform,
    openid,
    unionid
)
SELECT
    'USER',
    id,
    'WECHAT',
    'MINI_PROGRAM',
    openid,
    NULL
FROM user
WHERE openid IS NOT NULL
  AND openid <> ''
ON DUPLICATE KEY UPDATE
    principal_id = VALUES(principal_id),
    update_time = CURRENT_TIMESTAMP;
