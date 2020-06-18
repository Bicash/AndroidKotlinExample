package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.log

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String
    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login!!

    private lateinit var salt: String

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ): this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        passwordHash = encrypt(password)
    }

    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ): this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        println("Phone passwordHash is $passwordHash")
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }

    //for csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        salt: String,
        passwordHash: String,
        rawPhone: String?
    ): this(firstName, lastName, email = email, rawPhone = rawPhone, meta = mapOf("src" to "csv")) {
        println("Secondary csv constructor")
        this.salt = salt
        this.passwordHash = passwordHash
    }

    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) { "FirstName must be not blank"}
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }

        salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()

        phone = rawPhone
        login = when {
            email != null -> email
            Regex("\\+\\d{11}").containsMatchIn(phone.toString()) ->  Regex("[-()\\s]").replace(phone!!, "")
            else -> throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        }

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkAccessCode(accessCode: String) = this.accessCode == accessCode

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changeAccessCode() { this.accessCode = generateAccessCode()}

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt((newPass))
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

    private fun encrypt(password: String): String = salt.plus(password).md5() //good

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun checkPhoneNumber(phone: String?) : Boolean {
        return Regex("\\+\\d{11}").containsMatchIn(phone.toString())
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code: $code on $phone")
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) //16byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
        }

        fun makeFromCsvUser(
            fullName: String?,
            email: String?,
            salt: String,
            passwordHash: String,
            phone: String?
        ): User {
            val (firstName, lastName) = fullName!!.fullNameToPair()

            val _email = if (email!!.isBlank()) null else email
            val _phone = if (phone!!.isBlank()) null else phone

            return User(firstName, lastName, _email, salt, passwordHash, _phone)
        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when(size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("Fullname must contain only first name " +
                                "and last name, current split result $this")
                    }
                }
        }
    }

}