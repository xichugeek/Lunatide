package com.xichugeek.lunatide.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.ui.theme.LunatideTheme

/** 保留清晰选中语义和完整触区的轻量文字导航。 */
@Composable
fun QuietNavigation(selected: Int, onSelect: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().selectableGroup()) {
                    listOf(R.string.today, R.string.calendar, R.string.statistics).forEachIndexed { index, label ->
                        Box(
                            Modifier.weight(1f).heightIn(min = 56.dp)
                                .selectable(selected == index, role = Role.Tab, onClick = { onSelect(index) })
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium,
                                color = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected == index) FontWeight.Medium else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { QuietNavigation(0, {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { QuietNavigation(1, {}) } }
