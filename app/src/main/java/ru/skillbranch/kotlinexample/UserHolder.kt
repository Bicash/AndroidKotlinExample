package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        val user1 = User.makeUser(fullName, email = email, password = password)
        map.forEach {
            if (it.value.login == user1.login) {
                throw IllegalArgumentException("A user with this email already exists")
            }
        }
        return user1.also { user -> map[user.login] = user }
    }

    fun loginUser(login: String, password: String): String?{
        return map[Regex("[-()\\s]").replace(login, "")]?.run {
            if (phone != null) {
                if (checkAccessCode(password)) this.userInfo
                else null
            } else {
                if (checkPassword(password)) this.userInfo
                else null
            }
        }
    }

    fun registerUserByPhone(fullName: String, phone: String): User {
        val user1 = User.makeUser(fullName, phone = phone)
        map.forEach{
            if (it.value.login == user1.login)
                throw IllegalArgumentException("A user with this phone already exists")
        }
        return user1.also { user -> map[user.login] = user }
    }

    fun requestAccessCode(login: String) {
        map.forEach{
            if (it.value.login == Regex("[-()\\s]").replace(login, ""))
                it.value.changeAccessCode()
        }
    }

//    fun importUsers(list: List<String>) : List<User> {
//
//    }


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder(){
        map.clear()
    }
}