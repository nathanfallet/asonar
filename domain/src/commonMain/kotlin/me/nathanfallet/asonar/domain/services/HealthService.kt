package me.nathanfallet.asonar.domain.services

/** Reports whether the moving parts the app depends on are reachable. */
interface HealthService {

    /** Each component the app needs (database, messaging…), mapped to whether it is up. */
    fun check(): Map<String, Boolean>

}
