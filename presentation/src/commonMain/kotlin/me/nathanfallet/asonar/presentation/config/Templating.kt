package me.nathanfallet.asonar.presentation.config

import freemarker.cache.ClassTemplateLoader
import freemarker.core.HTMLOutputFormat
import io.ktor.server.application.*
import io.ktor.server.freemarker.*

fun Application.configureTemplating() {
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        outputFormat = HTMLOutputFormat.INSTANCE
        // Raw numbers by default (no locale grouping): FreeMarker otherwise renders ${id} as e.g.
        // "1,312", which silently breaks IDs in URLs and numeric data-sort attributes. Formatting is
        // opt-in per value (?string(",##0")) when a grouped display is actually wanted.
        numberFormat = "computer"
    }
}
