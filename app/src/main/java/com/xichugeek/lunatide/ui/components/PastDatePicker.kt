package com.xichugeek.lunatide.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import java.time.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePicker(today: LocalDate, initial: LocalDate, minimum: LocalDate? = null, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val state = remember(today, initial, minimum, locale) { DatePickerState(
        locale = locale,
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return day <= today && (minimum == null || day >= minimum)
            }
        },
    ) }
    DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(onClick = { state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } }, enabled = state.selectedDateMillis != null) {
            Text(stringResource(R.string.confirm))
        }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }) {
        DatePicker(state, showModeToggle = false, title = { Text(stringResource(R.string.choose_date), modifier = androidx.compose.ui.Modifier.padding(24.dp)) })
    }
}
