package io.waggle.waggleapiserver.security.oauth2

interface OAuth2UserInfo {
    val provider: String
    val providerId: String
    val email: String
    val name: String
    val profileImageUrl: String?
}
