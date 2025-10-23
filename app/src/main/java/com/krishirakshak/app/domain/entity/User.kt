package com.krishirakshak.app.domain.entity

/**
 * Domain entity representing a User.
 * This is the business logic representation, independent of data sources.
 */
data class User(
    val id: String,
    val phoneNumber: String,
    val name: String? = null,
    val createdAt: Long,
    val lastLogin: Long
)
