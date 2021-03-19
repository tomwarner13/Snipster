package com.okta.demo.ktor.schema

data class ClientMessage (
    val changetype: ChangeType,
    val snip: SnipDc)