package ua.kpi.abitsearch.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ua.kpi.abitsearch.ui.components.GradientBackground
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.kpi.abitsearch.AbitSearchApp
import ua.kpi.abitsearch.domain.model.AuthSession
import ua.kpi.abitsearch.domain.model.EducationLevel
import ua.kpi.abitsearch.ui.appViewModel
import ua.kpi.abitsearch.ui.auth.AuthViewModel
import ua.kpi.abitsearch.ui.auth.LoginScreen
import ua.kpi.abitsearch.ui.auth.RegisterScreen
import ua.kpi.abitsearch.ui.screens.FavoritesScreen
import ua.kpi.abitsearch.ui.screens.ProgramScreen
import ua.kpi.abitsearch.ui.screens.SpecialtiesScreen
import ua.kpi.abitsearch.ui.screens.SpecialtyScreen
import ua.kpi.abitsearch.ui.screens.SettingsScreen
import ua.kpi.abitsearch.ui.screens.UniversitiesScreen
import ua.kpi.abitsearch.ui.screens.UniversityScreen
import ua.kpi.abitsearch.ui.theme.Abit

/** Маршрути застосунку. Кожна вкладка - окремий вкладений граф зі своїм стеком. */
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val TAB_UNIVERSITIES = "tab_universities"
    const val TAB_SPECIALTIES = "tab_specialties"
    const val TAB_FAVORITES = "tab_favorites"
    const val TAB_SETTINGS = "tab_settings"

    const val UNIVERSITIES = "universities"
    const val SPECIALTIES = "specialties"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val SPECIALTY = "s/specialty?key={key}&level={level}"

    /** prefix - вкладка (u, s, f), щоб маршрути в різних вкладених графах були унікальні. */
    fun universityPattern(prefix: String) = "$prefix/university/{id}"
    fun programPattern(prefix: String) = "$prefix/program/{id}"
    fun university(prefix: String, id: Long) = "$prefix/university/$id"
    fun program(prefix: String, id: Long) = "$prefix/program/$id"
    /** Ключ спеціальності - текст (кирилиця, пробіли, апострофи), тому кодується для URI. */
    fun specialty(key: String, level: EducationLevel) = "s/specialty?key=${Uri.encode(key)}&level=${level.name}"
}

private data class Tab(val graph: String, val start: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TAB_UNIVERSITIES, Routes.UNIVERSITIES, "Університети", Icons.Outlined.School),
    Tab(Routes.TAB_SPECIALTIES, Routes.SPECIALTIES, "Спеціальності", Icons.AutoMirrored.Outlined.MenuBook),
    Tab(Routes.TAB_FAVORITES, Routes.FAVORITES, "Обране", Icons.Outlined.StarOutline),
    Tab(Routes.TAB_SETTINGS, Routes.SETTINGS, "Налаштування", Icons.Outlined.Settings),
)

private val idArg = listOf(navArgument("id") { type = NavType.LongType })

/**
 * Кореневий екран: залежно від стану сесії показує форми входу/реєстрації або основний застосунок.
 * Після входу токен потрапляє в DataStore, session емітить LoggedIn - і UI перемикається сам.
 */
@Composable
fun AppRoot() {
    val app = LocalContext.current.applicationContext as AbitSearchApp
    val session by app.container.authRepository.session.collectAsStateWithLifecycle(AuthSession.Loading)
    GradientBackground {
        when (session) {
            AuthSession.Loading -> Unit
            is AuthSession.LoggedIn -> AppNavigation()
            AuthSession.LoggedOut, AuthSession.Expired -> AuthNavigation(expired = session is AuthSession.Expired)
        }
    }
}

/** Навігація між формами входу та реєстрації. */
@Composable
private fun AuthNavigation(expired: Boolean) {
    val nav = rememberNavController()
    val vm = appViewModel { AuthViewModel(it.authRepository) }
    if (expired) {
        // Токен у сховищі протермінований: прибираємо його й показуємо повідомлення.
        val app = LocalContext.current.applicationContext as AbitSearchApp
        LaunchedEffect(Unit) { app.container.authRepository.expireSession() }
    }
    NavHost(nav, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) { LoginScreen(vm, onRegister = { nav.navigate(Routes.REGISTER) }) }
        composable(Routes.REGISTER) { RegisterScreen(vm, onLogin = { nav.popBackStack() }) }
    }
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val density = LocalDensity.current
    val topInset = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val padding = PaddingValues(top = topInset + 8.dp, bottom = bottomInset + 96.dp)

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = Routes.TAB_UNIVERSITIES, modifier = Modifier.fillMaxSize()) {
            navigation(startDestination = Routes.UNIVERSITIES, route = Routes.TAB_UNIVERSITIES) {
                composable(Routes.UNIVERSITIES) {
                    UniversitiesScreen(onOpen = { nav.navigate(Routes.university("u", it)) }, contentPadding = padding)
                }
                detailScreens("u", nav, padding)
            }
            navigation(startDestination = Routes.SPECIALTIES, route = Routes.TAB_SPECIALTIES) {
                composable(Routes.SPECIALTIES) {
                    SpecialtiesScreen(onOpen = { nav.navigate(Routes.specialty(it.key, it.level)) }, contentPadding = padding)
                }
                composable(
                    Routes.SPECIALTY,
                    listOf(
                        navArgument("key") { type = NavType.StringType },
                        navArgument("level") { type = NavType.StringType },
                    ),
                ) { entry ->
                    SpecialtyScreen(
                        key = entry.arguments?.getString("key").orEmpty(),
                        level = EducationLevel.from(entry.arguments?.getString("level")),
                        onBack = nav::popBackStack,
                        onOpenProgram = { nav.navigate(Routes.program("s", it)) },
                        contentPadding = padding,
                    )
                }
                detailScreens("s", nav, padding)
            }
            navigation(startDestination = Routes.FAVORITES, route = Routes.TAB_FAVORITES) {
                composable(Routes.FAVORITES) {
                    FavoritesScreen(
                        onOpenProgram = { nav.navigate(Routes.program("f", it)) },
                        onOpenUniversity = { nav.navigate(Routes.university("f", it)) },
                        onFindUniversity = { nav.switchTab(Routes.TAB_UNIVERSITIES) },
                        contentPadding = padding,
                    )
                }
                detailScreens("f", nav, padding)
            }
            navigation(startDestination = Routes.SETTINGS, route = Routes.TAB_SETTINGS) {
                composable(Routes.SETTINGS) { SettingsScreen(padding) }
            }
        }
        GlassTabBar(
            nav = nav,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

/**
 * Екрани університету й програми доступні з кількох вкладок (перехресна навігація),
 * тому реєструються в кожному вкладеному графі під унікальним маршрутом.
 */
private fun androidx.navigation.NavGraphBuilder.detailScreens(prefix: String, nav: NavHostController, padding: PaddingValues) {
    composable(Routes.universityPattern(prefix), idArg) { entry ->
        UniversityScreen(entry.arguments!!.getLong("id"), nav::popBackStack, { nav.navigate(Routes.program(prefix, it)) }, padding)
    }
    composable(Routes.programPattern(prefix), idArg) { entry ->
        ProgramScreen(entry.arguments!!.getLong("id"), nav::popBackStack, { nav.navigate(Routes.university(prefix, it)) }, padding)
    }
}

private fun NavHostController.switchTab(graph: String) {
    navigate(graph) {
        popUpTo(this@switchTab.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun GlassTabBar(nav: NavHostController, modifier: Modifier = Modifier) {
    val c = Abit.colors
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(shape)
            .background(if (c.isDark) Color(0xFF1E183A).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f))
            .border(1.dp, c.outline, shape)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = current?.hierarchy?.any { it.route == tab.graph } == true
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) c.glassStrong else Color.Transparent)
                    .clickable {
                        if (selected) {
                            // Повторний дотик повертає до кореневого екрана вкладки (п. 2.6).
                            nav.popBackStack(tab.start, inclusive = false)
                        } else {
                            nav.switchTab(tab.graph)
                        }
                    }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(tab.icon, tab.label, tint = if (selected) c.primary else c.faint, modifier = Modifier.size(22.dp))
                Text(tab.label, fontSize = 9.5.sp, color = if (selected) c.text else c.faint, maxLines = 1, softWrap = false)
            }
        }
    }
}
