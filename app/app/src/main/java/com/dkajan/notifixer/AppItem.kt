package com.dkajan.notifixer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class AppItem(
    val name: String,
    val packageName: String,
    val isSystem: Boolean,
    val installTime: Long,
    val lastUpdateTime: Long,
    val isEnabled: MutableState<Boolean> = mutableStateOf(false)
)

enum class SortType {
    AppName,
    PackageName,
    InstallDate,
    UpdateDate
}