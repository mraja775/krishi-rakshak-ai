package com.krishirakshak.app.data.mapper

import com.krishirakshak.app.data.local.entity.UserEntity
import com.krishirakshak.app.domain.entity.User

/**
 * Mapper functions to convert between data layer entities and domain layer entities.
 * This maintains separation between layers and allows independent evolution.
 */

/**
 * Convert UserEntity (data layer) to User (domain layer)
 */
fun UserEntity.toDomain(): User {
    return User(
        id = id,
        phoneNumber = phoneNumber,
        name = name,
        createdAt = createdAt,
        lastLogin = lastLogin
    )
}

/**
 * Convert User (domain layer) to UserEntity (data layer)
 */
fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        phoneNumber = phoneNumber,
        name = name,
        createdAt = createdAt,
        lastLogin = lastLogin
    )
}

/**
 * Convert list of UserEntity to list of User
 */
fun List<UserEntity>.toDomain(): List<User> {
    return map { it.toDomain() }
}
