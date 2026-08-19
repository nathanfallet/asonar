package me.nathanfallet.asonar.domain.services

/** Reports whether the moving parts the app depends on are reachable. */
interface HealthService {

    /** Returns true if everything the app needs (currently: the database) is up. */
    fun isHealthy(): Boolean

}
