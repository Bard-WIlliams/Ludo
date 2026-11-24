package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun RulesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nordic Ludo Rules",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RuleSection(
                    title = "Play vs AI & Pass & Play (2–4 Players)",
                    body = "Play solo against smart AI bots (Casual, Tactical, or Master), pass-and-play with up to 4 friends, or mix human and AI seats freely. Roll a 6 to deploy a token from your yard; rolling a 6, capturing an opponent, or finishing a token grants a bonus roll."
                )
                RuleSection(
                    title = "Safe Squares",
                    body = "Colored start squares and star tiles are safe zones where no captures can happen. Multiple tokens can share safe tiles peacefully."
                )
                RuleSection(
                    title = "2v2 Adjacent-Corner Teams",
                    body = "Top corners (Red + Green) form Team North against bottom corners (Blue + Yellow) in Team South. Teammates never capture each other, and standing together on any perimeter tile creates a Shared Safe Zone shielded from opponents. First team with all 8 tokens finished wins."
                )
                RuleSection(
                    title = "LAN Multiplayer",
                    body = "Connect devices to the same Wi-Fi network or mobile hotspot. Nearby rooms appear automatically, or you can join directly using the host's IP address and port."
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("rules_close_button")
            ) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun RuleSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
