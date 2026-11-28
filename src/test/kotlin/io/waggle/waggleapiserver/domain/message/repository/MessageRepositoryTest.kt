package io.waggle.waggleapiserver.domain.message.repository

import io.waggle.waggleapiserver.common.infrastructure.config.QuerydslConfig
import io.waggle.waggleapiserver.domain.message.Message
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.UserRole
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import io.waggle.waggleapiserver.support.DatabaseCleaner
import io.waggle.waggleapiserver.support.TestcontainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@DataJpaTest
@ActiveProfiles("mysql-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(QuerydslConfig::class, TestcontainersConfig::class, DatabaseCleaner::class)
class MessageRepositoryTest
    @Autowired
    constructor(
        private val messageRepository: MessageRepository,
        private val userRepository: UserRepository,
        private val databaseCleaner: DatabaseCleaner,
    ) {
        private lateinit var me: User
        private lateinit var partner: User
        private lateinit var messages: List<Message>

        @BeforeEach
        fun setUp() {
            cleanup()

            me = userRepository.save(createUser("me"))
            partner = userRepository.save(createUser("partner"))
            val other = userRepository.save(createUser("other"))

            messages =
                listOf(
                    Message(senderId = me.id, receiverId = partner.id, content = "1"),
                    Message(senderId = partner.id, receiverId = me.id, content = "2"),
                    Message(senderId = me.id, receiverId = other.id, content = "3"),
                    Message(senderId = partner.id, receiverId = me.id, content = "4"),
                    Message(senderId = me.id, receiverId = partner.id, content = "5"),
                    Message(senderId = partner.id, receiverId = me.id, content = "6"),
                ).map { messageRepository.save(it) }

            messageRepository.save(messages[3].also { it.delete() })
        }

        @AfterEach
        fun cleanup() {
            databaseCleaner.clean()
        }

        @Test
        fun `이전 메시지는 두 방향을 합쳐 최신순으로 limit만큼 조회하고 다른 대화와 삭제된 메시지는 제외`() {
            val history = messageRepository.findMessageHistoryBefore(me.id, partner.id, null, 3)

            assertThat(history.map { it.content }).containsExactly("6", "5", "2")
        }

        @Test
        fun `이전 메시지는 커서보다 작은 id만 조회`() {
            val history = messageRepository.findMessageHistoryBefore(me.id, partner.id, messages[4].id, 10)

            assertThat(history.map { it.content }).containsExactly("2", "1")
        }

        @Test
        fun `이후 메시지는 커서보다 큰 id를 오래된 순으로 limit만큼 조회`() {
            val history = messageRepository.findMessageHistoryAfter(me.id, partner.id, messages[0].id, 2)

            assertThat(history.map { it.content }).containsExactly("2", "5")
        }

        @Test
        fun `이후 메시지는 커서가 없으면 처음부터 조회`() {
            val history = messageRepository.findMessageHistoryAfter(me.id, partner.id, null, 10)

            assertThat(history.map { it.content }).containsExactly("1", "2", "5", "6")
        }

        private fun createUser(name: String): User =
            User(
                provider = "google",
                providerId = name,
                email = "$name@test.com",
                profileImageUrl = null,
                role = UserRole.USER,
            )
    }
