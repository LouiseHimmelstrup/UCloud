package dk.sdu.cloud.auth.services

import dk.sdu.cloud.Role
import dk.sdu.cloud.auth.api.AuthServiceDescription
import dk.sdu.cloud.auth.api.Person
import dk.sdu.cloud.auth.api.ServicePrincipal
import dk.sdu.cloud.auth.services.saml.SamlRequestProcessor
import dk.sdu.cloud.auth.testUtil.dbTruncate
import dk.sdu.cloud.micro.HibernateFeature
import dk.sdu.cloud.micro.hibernateDatabase
import dk.sdu.cloud.micro.install
import dk.sdu.cloud.service.db.DBSessionFactory
import dk.sdu.cloud.service.db.HibernateSession
import dk.sdu.cloud.service.db.async.AsyncDBSessionFactory
import dk.sdu.cloud.service.db.async.insert
import dk.sdu.cloud.service.db.async.withSession
import dk.sdu.cloud.service.db.withTransaction
import dk.sdu.cloud.service.test.initializeMicro
import io.mockk.every
import io.mockk.mockk
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.coroutines.runBlocking
import org.hibernate.NonUniqueObjectException
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserHibernateDAOTest {
    companion object {
        lateinit var db: AsyncDBSessionFactory
        lateinit var embDB: EmbeddedPostgres

        @BeforeClass
        @JvmStatic
        fun setup() {
            val (db, embDB) = TestDB.from(AuthServiceDescription)
            this.db = db
            this.embDB = embDB
        }

        @AfterClass
        @JvmStatic
        fun close() {
            runBlocking {
                db.close()
            }
            embDB.close()
        }
    }

    @BeforeTest
    fun before() {
        dbTruncate(db)

        passwordHashingService = PasswordHashingService()
        userHibernate = UserAsyncDAO(passwordHashingService, TwoFactorAsyncDAO())
        personService = PersonService(passwordHashingService, UniqueUsernameService(db, userHibernate))

        person = personService.createUserByPassword(
            "FirstName Middle",
            "Lastname",
            email,
            Role.ADMIN,
            "ThisIsMyPassword",
            email
        )

        person2 = personService.createUserByPassword(
            "McFirstName McMiddle",
            "McLastname",
            email2,
            Role.USER,
            "Password1234",
            email2
        )
    }

    @AfterTest
    fun after() {
        dbTruncate(db)
    }

    private lateinit var passwordHashingService: PasswordHashingService
    private lateinit var personService: PersonService
    private lateinit var userHibernate: UserAsyncDAO

    private val email = "test@testmail.com"
    private lateinit var person: Person
    private val email2 = "anotherEmail@test.com"
    private lateinit var person2: Person

    @Test
    fun `insert, find and delete`(): Unit = runBlocking {
        db.withTransaction { session ->
            val userHibernate = UserAsyncDAO(passwordHashingService, TwoFactorAsyncDAO())
            userHibernate.insert(session, person)
            assertEquals(email, userHibernate.findById(session, email).id)
            userHibernate.delete(session, email)
            assertNull(userHibernate.findByIdOrNull(session, email))
        }
    }

    @Test(expected = NonUniqueObjectException::class)
    fun `insert 2 with same email`(): Unit = runBlocking {
        db.withTransaction { session ->
            val userHibernate = UserAsyncDAO(passwordHashingService, TwoFactorAsyncDAO())
            userHibernate.insert(session, person)
            userHibernate.insert(session, person)

        }
    }

    @Test(expected = UserException.NotFound::class)
    fun `delete non existing user`(): Unit = runBlocking {
        val session = db.openSession()
        val userHibernate = UserAsyncDAO(passwordHashingService, TwoFactorAsyncDAO())
        userHibernate.delete(session, "test@testmail.com")
    }

    @Test
    fun `insert WAYF`(): Unit = runBlocking {
        val auth = mockk<SamlRequestProcessor>()
        val userDao = UserAsyncDAO(passwordHashingService, TwoFactorAsyncDAO())
        every { auth.authenticated } returns true
        every { auth.attributes } answers {
            val h = HashMap<String, List<String>>(10)
            h.put("eduPersonTargetedID", listOf("hello"))
            h.put("gn", listOf("Firstname"))
            h.put("sn", listOf("Lastname"))
            h.put("schacHomeOrganization", listOf("sdu.dk"))
            h
        }

        val person = personService.createUserByWAYF(auth)
        db.withTransaction { session ->
            userDao.insert(session, person)
        }
        assertEquals("sdu.dk", person.organizationId)
    }

    @Test
    fun `toggle emails`() {
        val email = "test@testmail.com"
        val person = personService.createUserByPassword(
            "FirstName Middle",
            "Lastname",
            email,
            Role.ADMIN,
            "ThisIsMyPassword",
            email = email
        )
        runBlocking {
            db.withTransaction { session ->
                userHibernate.insert(session, person)

                assertTrue(userHibernate.wantEmails(session, person.id))

                userHibernate.toggleEmail(session, person.id)

                assertFalse(userHibernate.wantEmails(session, person.id))

                userHibernate.toggleEmail(session, person.id)


            }
        }
    }
}
