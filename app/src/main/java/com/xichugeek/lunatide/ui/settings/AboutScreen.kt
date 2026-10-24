package com.xichugeek.lunatide.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.BuildConfig
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import com.xichugeek.lunatide.ui.components.PageHeader

@Composable
fun AboutScreen(onDocument: (String) -> Unit, onCopy: (String) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader(stringResource(R.string.about), onBack)
            Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) }
        item { Text(stringResource(R.string.promise_local)); Text(stringResource(R.string.offline_explanation)) }
        item { TextButton(onClick = { onDocument("privacy") }) { Text(stringResource(R.string.privacy_policy)) } }
        item { TextButton(onClick = { onDocument("licenses") }) { Text(stringResource(R.string.open_source_licenses)) } }
        item { TextButton(onClick = { onDocument("health") }) { Text(stringResource(R.string.health_declaration)) } }
        item { Text(stringResource(R.string.offline_update)) }
        item { CopyAddress("https://xichugeek.com/projects/lunatide/", onCopy) }
        item { CopyAddress("https://github.com/xichugeek/Lunatide/releases", onCopy) }
        item { CopyAddress("https://github.com/xichugeek/Lunatide", onCopy) }
    }
}
@Composable private fun CopyAddress(address: String, onCopy: (String) -> Unit) {
    Column { Text(address, style = MaterialTheme.typography.bodyMedium); TextButton(onClick = { onCopy(address) }) { Text(stringResource(R.string.copy_address)) } }
}
@Composable
fun LocalDocumentScreen(title: String, text: String, onBack: () -> Unit) {
    val paragraphs = text.split("\n\n")
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader(title, onBack) }
        items(paragraphs.size) { index -> Text(paragraphs[index]) }
    }
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { AboutScreen({}, {}, {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { AboutScreen({}, {}, {}) } }
@Preview(locale = "zh") @Composable private fun DocumentLightPreview() { LunatideTheme(false) { LocalDocumentScreen("", stringResource(R.string.health_full), {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DocumentDarkPreview() { LunatideTheme(true) { LocalDocumentScreen("", stringResource(R.string.health_full), {}) } }
