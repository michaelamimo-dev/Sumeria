package com.michaelamimo.sumeria.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolumeUp
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.DisposableEffect

@Composable
fun DictionaryLookupDialog(
    state: DictionaryLookupState,
    onLookup: (String) -> Unit,
    onDismiss: () -> Unit,
    background: Color,
    foreground: Color,
    accent: Color
) {
    var word by remember(state.word) { mutableStateOf(state.word) }
    val context = LocalContext.current
    val speech = remember(context) { TextToSpeech(context) {} }
    DisposableEffect(speech) { onDispose { speech.stop(); speech.shutdown() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = background,
        titleContentColor = foreground,
        textContentColor = foreground,
        title = { Text("Dictionary", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Word") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Search
                    ),
                    enabled = !state.isLoading
                )
                when {
                    state.isLoading -> Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) { CircularProgressIndicator(color = accent) }
                    state.error != null -> Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    state.entry != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(state.entry.word, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = foreground)
                                state.entry.phonetic?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = foreground.copy(alpha = 0.68f))
                                }
                            }
                            IconButton(onClick = { speech.speak(state.entry.word, TextToSpeech.QUEUE_FLUSH, null, "dictionary-pronunciation") }) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = "Pronounce ${state.entry.word}", tint = accent)
                            }
                        }
                        state.entry.definitions.forEachIndexed { index, definition ->
                            Surface(color = accent.copy(alpha = 0.10f), shape = MaterialTheme.shapes.medium) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    definition.partOfSpeech?.let {
                                        Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = accent)
                                        Spacer(Modifier.height(3.dp))
                                    }
                                    Text("${index + 1}. ${definition.meaning}", style = MaterialTheme.typography.bodyMedium, color = foreground)
                                    definition.example?.let {
                                        Spacer(Modifier.height(5.dp))
                                        Text("“$it”", style = MaterialTheme.typography.bodySmall, color = foreground.copy(alpha = 0.72f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onLookup(word) }, enabled = word.isNotBlank() && !state.isLoading) {
                Text("Look up", color = accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = foreground) }
        }
    )
}
