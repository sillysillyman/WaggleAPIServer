package io.waggle.waggleapiserver.security.oauth2

class KakaoUserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    private val kakaoAccount: Map<*, *>
        get() = attributes["kakao_account"] as Map<*, *>

    private val profile: Map<*, *>
        get() = kakaoAccount["profile"] as Map<*, *>

    override val provider = "kakao"
    override val providerId: String
        get() = attributes["id"].toString()
    override val email: String
        get() = kakaoAccount["email"] as String
    override val name: String
        get() = profile["nickname"] as String
    override val profileImageUrl: String?
        get() = profile["profile_image_url"] as? String
}
