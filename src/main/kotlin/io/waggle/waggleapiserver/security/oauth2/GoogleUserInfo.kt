package io.waggle.waggleapiserver.security.oauth2

class GoogleUserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override val provider = "google"
    override val providerId: String
        get() = attributes["sub"] as String
    override val email: String
        get() = attributes["email"] as String
    override val name: String
        get() = attributes["name"] as String
    override val profileImageUrl: String?
        get() = attributes["picture"] as? String
}
