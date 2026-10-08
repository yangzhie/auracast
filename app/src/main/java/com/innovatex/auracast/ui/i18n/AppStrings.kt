package com.innovatex.auracast.ui.i18n

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.innovatex.auracast.R
import com.innovatex.auracast.data.Stop
import com.innovatex.auracast.data.TransitRoute
import com.innovatex.auracast.ui.theme.AppLanguage
import com.innovatex.auracast.ui.theme.LocalAccessibilitySettings
import java.util.Locale

/** .
 * The app does not change device-wide language or Bluetooth/USB configuration.
 */
@Composable
fun appString(@StringRes id: Int, vararg args: Any): String {
    val base = LocalContext.current
    val language = LocalAccessibilitySettings.current.language
    val localeContext = remember(base, language) {
        val locale = if (language == AppLanguage.CHINESE_SIMPLIFIED) {
            Locale.SIMPLIFIED_CHINESE
        } else {
            Locale.ENGLISH
        }
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        base.createConfigurationContext(configuration)
    }
    return if (args.isNotEmpty()) {
        localeContext.getString(id, *args)
    } else {
        localeContext.getString(id)
    }
}

@Composable
fun localizedRouteDestination(route: TransitRoute): String {
    return when (route.id) {
        "86-out" -> appString(R.string.route_to_bundoora)
        "96-out" -> appString(R.string.route_to_brunswick)
        else -> route.destination
    }
}

@Composable
fun localizedCoverageSummary(route: TransitRoute): String {
    return if (route.coveredStopCount == 0) {
        appString(R.string.route_no_stops)
    } else {
        appString(R.string.route_coverage_summary, route.coveredStopCount, route.totalStopCount)
    }
}

@Composable
fun localizedStopName(stop: Stop): String {
    return when (stop.id) {
        "s8" -> appString(R.string.stop_parliament)
        "s10" -> appString(R.string.stop_nicholson)
        "s12" -> appString(R.string.stop_gertrude)
        "s13" -> appString(R.string.stop_langridge)
        "s15" -> appString(R.string.stop_johnston)
        "s17" -> appString(R.string.stop_leicester)
        "s20" -> appString(R.string.stop_westgarth)
        else -> stop.name
    }
}
