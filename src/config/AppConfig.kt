package com.okta.demo.ktor.config
import com.okta.demo.ktor.config.OktaConfig.Companion.oktaConfigReader
import com.typesafe.config.Config

data class AppConfig(
    val oktaConfig: OktaConfig,
    val envType: EnvType,
    val host: String,
    val databaseConfig: DatabaseConfig,
    val sessionEncryptionConfig: SessionEncryptionConfig
) {
    companion object {
        fun from(config: Config) : AppConfig = AppConfig(
            oktaConfig = oktaConfigReader(config),
            envType = config.getEnum(EnvType::class.java, "global.envType"),
            host = config.getString("global.host"),
            databaseConfig = DatabaseConfig(
                config.getString("database.url"),
                config.getString("database.driver")
            ),
            sessionEncryptionConfig = SessionEncryptionConfig(
                config.getString("encryption.encryptKey"),
                config.getString("encryption.authKey")
            )
        )
    }
}
