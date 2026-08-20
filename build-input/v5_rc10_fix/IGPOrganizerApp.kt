package com.infinitygreenpower.organizerform.app

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.infinitygreenpower.organizerform.R
import com.infinitygreenpower.organizerform.core.localization.LocalAppLanguage
import com.infinitygreenpower.organizerform.core.localization.localizedString
import com.infinitygreenpower.organizerform.core.localization.normalizeLanguage
import com.infinitygreenpower.organizerform.data.preferences.AppPreferences
import com.infinitygreenpower.organizerform.data.preferences.Settings as AppSettings
import com.infinitygreenpower.organizerform.feature.catalog.CatalogScreen
import com.infinitygreenpower.organizerform.feature.form.FormScreen
import com.infinitygreenpower.organizerform.feature.home.HomeScreen
import com.infinitygreenpower.organizerform.feature.saved.SavedFormsScreen
import com.infinitygreenpower.organizerform.feature.settings.SettingsScreen

private enum class Destination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Home("home", R.string.home, Icons.Outlined.Home),
    NewForm("new", R.string.new_form, Icons.Outlined.AddCircleOutline),
    Saved("saved", R.string.saved, Icons.Outlined.FolderOpen),
    Catalog("catalog", R.string.catalog, Icons.Outlined.Inventory2),
    Settings("settings", R.string.settings, Icons.Outlined.Settings)
}

@Composable
fun IGPOrganizerApp() {
    val context = LocalContext.current
    val preferences = remember(context) { AppPreferences(context.applicationContext) }
    val settings by preferences.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val language = normalizeLanguage(settings.language)
    val direction: LayoutDirection = if (language == "ar" || language == "ckb") Rtl else Ltr

    // Language is app-owned Compose state. No Activity recreation means no black transition,
    // and the current navigation/draft state remains intact while switching language.
    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalLayoutDirection provides direction
    ) {
        OrganizerNavigation()
    }
}

@Composable
private fun OrganizerNavigation() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val current = entry?.destination?.route ?: Destination.Home.route
    val selectedRoute = if (current == "edit/{formId}") Destination.Saved.route else current
    val expanded = LocalConfiguration.current.screenWidthDp >= 600
    val select: (Destination) -> Unit = { destination ->
        // An edit screen belongs to Saved Forms visually, but it must not become the
        // restored root of the Saved tab. Remove the child edit route before changing tabs
        // so tapping Saved always opens the Saved Forms list rather than the last form.
        val leavingSavedEdit = navController.currentDestination?.route == "edit/{formId}"
        if (leavingSavedEdit) {
            navController.popBackStack(Destination.Saved.route, inclusive = false)
        }

        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            // Never restore an old Saved -> Edit child stack when the user explicitly taps Saved.
            restoreState = destination != Destination.Saved
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (!expanded) NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = selectedRoute == item.route,
                        onClick = { select(item) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(localizedString(item.label)) }
                    )
                }
            }
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (expanded) NavigationRail {
                Destination.entries.forEach { item ->
                    NavigationRailItem(
                        selected = selectedRoute == item.route,
                        onClick = { select(item) },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(localizedString(item.label)) }
                    )
                }
            }
            Box(Modifier.fillMaxSize()) {
                NavHost(navController, startDestination = Destination.Home.route) {
                    composable(Destination.Home.route) {
                        HomeScreen(
                            onNew = { select(Destination.NewForm) },
                            onSaved = { select(Destination.Saved) },
                            onCatalog = { select(Destination.Catalog) },
                            onPreview = { select(Destination.NewForm) }
                        )
                    }
                    composable(Destination.NewForm.route) { FormScreen() }
                    composable(Destination.Saved.route) {
                        SavedFormsScreen(onEdit = { id -> navController.navigate("edit/$id") })
                    }
                    composable(
                        route = "edit/{formId}",
                        arguments = listOf(navArgument("formId") { type = NavType.StringType })
                    ) { editEntry ->
                        FormScreen(formId = editEntry.arguments?.getString("formId"))
                    }
                    composable(Destination.Catalog.route) { CatalogScreen(onOpenForm = { select(Destination.NewForm) }) }
                    composable(Destination.Settings.route) { SettingsScreen() }
                }
            }
        }
    }
}
