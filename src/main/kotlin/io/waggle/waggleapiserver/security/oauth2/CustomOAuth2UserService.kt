package io.waggle.waggleapiserver.security.oauth2

import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
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

        val user =
            userRepository.findByProviderAndProviderId(
                userInfo.provider,
                userInfo.providerId,
            ) ?: run {
                require(!userRepository.existsByEmail(userInfo.email)) {
                    "Already existing email: ${userInfo.email}"
                }
                userRepository.save(
                    User(
                        provider = userInfo.provider,
                        providerId = userInfo.providerId,
                        email = userInfo.email,
                        profileImageUrl = userInfo.profileImageUrl,
                    ),
                )
            }

        val attributes =
            oauth2User.attributes.toMutableMap().apply {
                put("userId", user.id.toString())
                put("email", user.email)
                put("role", user.role.name)
            }

        return DefaultOAuth2User(oauth2User.authorities, attributes, "email")
    }
}
