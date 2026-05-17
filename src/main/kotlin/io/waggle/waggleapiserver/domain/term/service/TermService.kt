package io.waggle.waggleapiserver.domain.term.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.term.UserTermAgreement
import io.waggle.waggleapiserver.domain.term.dto.request.TermsAgreeRequest
import io.waggle.waggleapiserver.domain.term.dto.response.TermResponse
import io.waggle.waggleapiserver.domain.term.repository.TermRepository
import io.waggle.waggleapiserver.domain.term.repository.UserTermAgreementRepository
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TermService(
    private val termRepository: TermRepository,
    private val userTermAgreementRepository: UserTermAgreementRepository,
) {
    @Transactional
    fun agreeToTerms(
        request: TermsAgreeRequest,
        user: User,
    ) {
        val currentTermByType =
            termRepository.findLatestActiveOrderByType().associateBy { it.type }

        val agreedTypeSet =
            request.agreements
                .filter { it.agreed }
                .map { it.type }
                .toSet()
        val missingMandatoryTypes =
            currentTermByType
                .filterValues { it.mandatory }
                .keys
                .filterNot { agreedTypeSet.contains(it) }
        if (missingMandatoryTypes.isNotEmpty()) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT_VALUE,
                "Missing mandatory term agreements: $missingMandatoryTypes",
            )
        }

        request.agreements
            .filter { it.agreed }
            .forEach { agreement ->
                val term =
                    currentTermByType[agreement.type]
                        ?: throw BusinessException(
                            ErrorCode.INVALID_INPUT_VALUE,
                            "Unknown term type: ${agreement.type}",
                        )
                if (userTermAgreementRepository.findByUserIdAndTermId(user.id, term.id) == null) {
                    userTermAgreementRepository.save(
                        UserTermAgreement(userId = user.id, termId = term.id),
                    )
                }
            }
    }

    fun getTerms(user: User?): List<TermResponse> {
        val agreedTermIdSet =
            user?.let { userTermAgreementRepository.findActiveTermIdsByUserId(it.id).toSet() }
                ?: emptySet()
        return termRepository
            .findLatestActiveOrderByType()
            .map { TermResponse.of(it, agreed = agreedTermIdSet.contains(it.id)) }
    }

    fun checkAllRequiredAgreed(user: User) {
        if (!hasAgreedToAllRequiredTerms(user)) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Required terms not agreed")
        }
    }

    private fun hasAgreedToAllRequiredTerms(user: User): Boolean {
        val requiredTermIds =
            termRepository
                .findLatestActiveOrderByType()
                .filter { it.mandatory }
                .map { it.id }
        if (requiredTermIds.isEmpty()) return true
        val agreedCount =
            userTermAgreementRepository.countActiveByUserIdAndTermIdIn(user.id, requiredTermIds)
        return agreedCount.toInt() == requiredTermIds.size
    }
}
