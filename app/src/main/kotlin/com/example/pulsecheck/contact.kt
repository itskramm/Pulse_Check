package com.example.pulsecheck

class contact(
    private val id: Int,
    private var name: String,
    private var affiliation: String,
    private var number: String,
    private var email: String,
    private var homeAddress: String,
    private var workAddress: String,
    private var otherAddress: String,
    private var imageResource: Int
) {
    constructor(
        id: Int,
        name: String,
        affiliation: String,
        number: String,
        homeAddress: String,
        workAddress: String,
        otherAddress: String,
        imageResource: Int
    ) : this(id, name, affiliation, number, "", homeAddress, workAddress, otherAddress, imageResource)
    constructor(
        id: Int, name: String, affiliation: String, number: String, email: String, imageResource: Int
    ) : this(id, name, affiliation, number, email, "", "", "", imageResource)
    constructor(
        name: String, affiliation: String, number: String, email: String, imageResource: Int
    ) : this(0, name, affiliation, number, email, "", "", "", imageResource)
    constructor(
        name: String, affiliation: String, number: String, imageResource: Int
    ) : this(0, name, affiliation, number, "", "", "", "", imageResource)

    fun getId() = id
    fun getName() = name
    fun getAffiliation() = affiliation
    fun getNumber() = number
    fun getEmail() = email
    fun getHomeAddress() = homeAddress
    fun getWorkAddress() = workAddress
    fun getOtherAddress() = otherAddress
    fun getImageResource() = imageResource
    fun setImageResource(res: Int) {
        imageResource = res
    }

    fun getPrimaryAddress(): String =
        listOf(homeAddress, workAddress, otherAddress)
            .firstOrNull { it.trim().isNotEmpty() }?.trim() ?: ""

    fun hasAddress() = getPrimaryAddress().isNotEmpty()
}
