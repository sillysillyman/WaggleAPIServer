package io.waggle.waggleapiserver.security.oauth2

interface OAuth2UserInfo {
    val provider: String
    val providerId: String
    val email: String?
    val isEmailVerified: Boolean
    val profileImageUrl: String?
}

class GoogleUserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override val provider = "google"
    override val providerId: String
        get() = attributes["sub"] as String
    override val email: String?
        get() = attributes["email"] as? String
    override val isEmailVerified: Boolean
        get() = attributes["email_verified"] as? Boolean ?: false
    override val profileImageUrl: String?
        get() = attributes["picture"] as? String
}

class KakaoUserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    private val kakaoAccount: Map<*, *>
        get() = attributes["kakao_account"] as Map<*, *>

    private val profile: Map<*, *>?
        get() = kakaoAccount["profile"] as? Map<*, *>

    override val provider = "kakao"
    override val providerId: String
        get() = attributes["id"].toString()
    override val email: String?
        get() = kakaoAccount["email"] as? String
    override val isEmailVerified: Boolean
        get() =
            (kakaoAccount["is_email_verified"] as? Boolean ?: false) &&
                (kakaoAccount["is_email_valid"] as? Boolean ?: false)
    override val profileImageUrl: String?
        get() = profile?.get("profile_image_url") as? String
}
