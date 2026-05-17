package io.waggle.waggleapiserver.security.oauth2

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.follow.repository.FollowRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
) : DefaultOAuth2UserService() {
    @Transactional
    override fun loadUser(request: OAuth2UserRequest): OAuth2User {
        val oauth2User = super.loadUser(request)
        val userInfo =
            when (val registrationId = request.clientRegistration.registrationId) {
                "google" -> GoogleUserInfo(oauth2User.attributes)

                "kakao" -> KakaoUserInfo(oauth2User.attributes)

                else -> throw oauth2AuthenticationException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "Unsupported provider: $registrationId",
                )
            }

        val email = userInfo.email
        if (email.isNullOrBlank()) {
            throw oauth2AuthenticationException(
                ErrorCode.OAUTH_EMAIL_MISSING,
                "Email not provided by ${userInfo.provider}",
            )
        }
        if (!userInfo.isEmailVerified) {
            throw oauth2AuthenticationException(
                ErrorCode.OAUTH_EMAIL_NOT_VERIFIED,
                "Email not verified by ${userInfo.provider}",
            )
        }

        val user =
            userRepository.findByProviderAndProviderId(
                userInfo.provider,
                userInfo.providerId,
            ) ?: run {
                val deletedUser =
                    userRepository.findByProviderAndProviderIdAndDeletedAtIsNotNull(
                        userInfo.provider,
                        userInfo.providerId,
                    )
                if (deletedUser != null) {
                    deletedUser.reactivate()
                    followRepository.updateDeletedAtNullByFollowerIdOrFolloweeIdAndDeletedAtIsNotNull(
                        deletedUser.id,
                    )
                    userRepository.save(deletedUser)
                } else {
                    if (userRepository.existsByEmail(email)) {
                        throw oauth2AuthenticationException(
                            ErrorCode.DUPLICATE_RESOURCE,
                            "Already existing email: $email",
                        )
                    }
                    userRepository.save(
                        User(
                            provider = userInfo.provider,
                            providerId = userInfo.providerId,
                            email = email,
                            profileImageUrl = userInfo.profileImageUrl,
                        ),
                    )
                }
            }

        val attributes =
            oauth2User.attributes.toMutableMap().apply {
                put("userId", user.id.toString())
                put("email", user.email)
                put("role", user.role.name)
            }

        return DefaultOAuth2User(oauth2User.authorities, attributes, "email")
    }

    private fun oauth2AuthenticationException(
        errorCode: ErrorCode,
        detail: String,
    ): OAuth2AuthenticationException =
        OAuth2AuthenticationException(
            OAuth2Error(errorCode.name, detail, null),
            detail,
            BusinessException(errorCode, detail),
        )
}
