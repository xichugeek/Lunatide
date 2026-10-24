package com.xichugeek.lunatide.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import kotlinx.serialization.Serializable
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.ui.theme.LunatideTheme

@Serializable data object Today
@Serializable data object Calendar
@Serializable data object Statistics

@Composable
fun LunatideRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    Scaffold(bottomBar = {
        NavigationBar {
            listOf(Triple(Today, R.string.today, "1"), Triple(Calendar, R.string.calendar, "2"), Triple(Statistics, R.string.statistics, "3")).forEach { (destination, label, symbol) ->
                NavigationBarItem(
                    selected = route == destination::class.qualifiedName,
                    onClick = { nav.navigate(destination) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                    icon = { Text(symbol) }, label = { Text(stringResource(label)) },
                )
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            NavHost(nav, startDestination = Today, modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                composable<Today> { Placeholder(R.string.welcome) }
                composable<Calendar> { Placeholder(R.string.calendar) }
                composable<Statistics> { Placeholder(R.string.statistics) }
            }
        }
    }
}

@Composable
private fun Placeholder(title: Int) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { LunatideRoot() } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { LunatideRoot() } }
