package com.xichugeek.lunatide.ui.components

import android.text.InputFilter
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** No personalized keyboard learning, correction, suggestions, or Android view-state persistence. */
@Composable
fun PrivateTextInput(value: String, hint: String, decimal: Boolean = false, enabled: Boolean = true, password: Boolean = false, onChange: (String) -> Unit) {
    val callback by rememberUpdatedState(onChange)
    val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    AndroidView(modifier = Modifier.fillMaxWidth().heightIn(min = if (decimal || password) 56.dp else 100.dp), factory = { context ->
        EditText(context).apply {
            isSaveEnabled = false
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            if (android.os.Build.VERSION.SDK_INT >= 30) importantForContentCapture = android.view.View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
            inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                else if (decimal) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            filters = arrayOf(InputFilter.LengthFilter(if (password) 256 else if (decimal) 8 else 500))
            setText(value)
            addTextChangedListener { callback(it?.toString().orEmpty()) }
        }
    }, update = { input ->
        input.isEnabled = enabled; input.hint = hint
        input.setTextColor(foreground); input.setHintTextColor(muted)
        if (input.text.toString() != value) { input.setText(value); input.setSelection(value.length.coerceAtMost(input.length())) }
    })
}
