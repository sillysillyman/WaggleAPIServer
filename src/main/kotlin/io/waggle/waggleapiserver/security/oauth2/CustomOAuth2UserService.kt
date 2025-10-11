package io.waggle.waggleapiserver.security.oauth2

import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
) : DefaultOAuth2UserService() {
    @Transactional
    override fun loadUser(request: OAuth2UserRequest): OAuth2User {
        val oauth2User = super.loadUser(request)
        val userInfo =
            when (val registrationId = request.clientRegistration.registrationId) {
                "google" -> GoogleUserInfo(oauth2User.attributes)
                "kakao" -> KakaoUserInfo(oauth2User.attributes)
                else -> throw IllegalArgumentException("Unsupported provider: $registrationId")
            }

        val user = getOrCreateUser(userInfo)

        return UserPrincipal(oauth2User, user)
    }

    private fun getOrCreateUser(userInfo: OAuth2UserInfo): User =
        userRepository.findByProviderAndProviderId(userInfo.provider, userInfo.providerId)
            ?: createUser(userInfo)

    private fun createUser(userInfo: OAuth2UserInfo): User {
        require(!userRepository.existsByEmail(userInfo.email)) {
            "Already existing email: ${userInfo.email}"
        }

        return userRepository.save(
            User(
                provider = userInfo.provider,
                providerId = userInfo.providerId,
                email = userInfo.email,
                profileImageUrl = userInfo.profileImageUrl,
            ),
        )
    }
}
