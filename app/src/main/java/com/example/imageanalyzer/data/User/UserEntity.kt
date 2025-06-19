package com.example.imageanalyzer.data.User

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val address: String
)