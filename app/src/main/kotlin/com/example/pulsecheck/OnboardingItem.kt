package com.example.pulsecheck

class OnboardingItem(
    private val header: String,
    private val imageResId: Int,
    private val description: String
) {
    fun getHeader() = header
    fun getImageResId() = imageResId
    fun getDescription() = description
}
