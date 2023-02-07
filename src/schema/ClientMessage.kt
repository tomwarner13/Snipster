package snipster.schema

data class ClientMessage (
    val changetype: ChangeType,
    val snip: SnipDc)