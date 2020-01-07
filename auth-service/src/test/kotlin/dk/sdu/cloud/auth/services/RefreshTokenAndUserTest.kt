package dk.sdu.cloud.auth.services

import dk.sdu.cloud.Role
import dk.sdu.cloud.service.db.withTransaction
import dk.sdu.cloud.service.test.withDatabase
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RefreshTokenAndUserTest {
    private val passwordHashingService = PasswordHashingService()
    private val personService = PersonService(passwordHashingService, mockk(relaxed = true))

    @Test
    fun `insert, find and delete`() {
        withDatabase { db ->
            runBlocking {
                val email = "test@testmail.com"
                val token = "tokenToGive"

                db.withTransaction { session ->
                    val person = personService.createUserByPassword(
                        "FirstName Middle",
                        "Lastname",
                        email,
                        Role.ADMIN,
                        "ThisIsMyPassword"
                    )
                    UserHibernateDAO(passwordHashingService).insert(session, person)
                    val refreshTAU = RefreshTokenAndUser(email, token, "")
                    val refreshHibernateTAU = RefreshTokenHibernateDAO()

                    refreshHibernateTAU.insert(session, refreshTAU)

                    assertEquals(email, refreshHibernateTAU.findById(session, token)?.associatedUser)
                    assertTrue(refreshHibernateTAU.delete(session, token))
                    assertNull(refreshHibernateTAU.findById(session, token))
                }
            }
        }
    }

    @Test(expected = UserException.NotFound::class)
    fun `insert - user not found`() {
        withDatabase { db ->
            runBlocking {
                db.withTransaction { session ->
                    val refreshTAU = RefreshTokenAndUser("non existing User", "token", "")
                    val refreshHibernateTAU = RefreshTokenHibernateDAO()
                    refreshHibernateTAU.insert(session, refreshTAU)
                }
            }
        }
    }

    @Test
    fun `delete - does not exist`() {
        withDatabase { db ->
            runBlocking {
                db.withTransaction { session ->
                    assertFalse(RefreshTokenHibernateDAO().delete(session, "token"))
                }
            }
        }
    }
}
