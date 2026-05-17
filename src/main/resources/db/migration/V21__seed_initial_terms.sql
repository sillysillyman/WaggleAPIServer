-- 초기 약관 5종 시드 (회원가입 시 동의 받을 약관)
-- content_url은 회원가입 화면에서 사용하는 Notion URL 기준
-- 약관 본문 갱신 시: 같은 type으로 새 version row INSERT + 구버전 deprecated_at UPDATE
INSERT INTO terms (type, version, content_url, mandatory, activated_at, created_at)
VALUES ('SERVICE', 1, 'https://www.notion.so/9cd51a2bbe9c4356a522c73bc6300a14', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       ('PRIVACY_COLLECT', 1, 'https://www.notion.so/ae3cc843672f42e0964e7824a816c3ba', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       ('PRIVACY_THIRD_PARTY', 1, 'https://www.notion.so/3-94a111aaf78a4ddc8899b03257c83729', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       ('PROFILE_DATA', 1, 'https://www.notion.so/4aea6ba5e04249d28a6eaf8bbb9c245a', TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
       ('MARKETING', 1, 'https://www.notion.so/04abd9f9bb434dad8f7950f80f4845aa', FALSE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
