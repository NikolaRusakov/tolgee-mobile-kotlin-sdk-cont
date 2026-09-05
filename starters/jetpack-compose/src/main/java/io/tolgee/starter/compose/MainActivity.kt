package io.tolgee.starter.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.tolgee.Tolgee
// These three replace androidx.compose.ui.res.* and recompose when translations or locale change.
import io.tolgee.pluralStringResource
import io.tolgee.stringArrayResource
import io.tolgee.stringResource
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // The window title is not a composable; drive it from tFlow like any other imperative string.
    Tolgee.instanceOrNull?.let { tolgee ->
      lifecycleScope.launch {
        tolgee.tFlow(this@MainActivity, R.string.app_name).collect { title = it }
      }
    }

    setContent {
      MaterialTheme {
        StarterScreen()
      }
    }
  }
}

@Composable
fun StarterScreen() {
  var passed by rememberSaveable { mutableIntStateOf(1) }
  val total = 10

  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Column(
      modifier = Modifier.padding(innerPadding).padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Static
      Text(stringResource(R.string.description))
      // %1$s
      Text(stringResource(R.string.percentage_placeholder, "87"))
      // "%3$s: %1$d test out of %2$d ok" -> quantity fills %1$d, then total, then the label.
      Text(pluralStringResource(R.plurals.plr_test_placeholder_2, passed, total, "Tests"))
      // String array
      Text(stringArrayResource(R.array.array_test).joinToString())

      Button(onClick = { passed = if (passed >= total) 1 else passed + 1 }) {
        Text(stringResource(R.string.action_next_result))
      }

      LocaleSwitcher()
    }
  }
}

@Composable
fun LocaleSwitcher() {
  val tolgee = Tolgee.instanceOrNull ?: return
  val locale by tolgee.changeFlow
    .map { tolgee.getLocale() }
    .collectAsState(initial = tolgee.getLocale())

  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    // Language names are deliberately not translated.
    listOf("en" to "English", "cs" to "Čeština", "fr" to "Français").forEach { (tag, label) ->
      Button(
        onClick = { tolgee.setLocale(tag) }, // stringResource composables reload on their own
        enabled = locale.language != tag,
      ) {
        Text(label)
      }
    }
  }
}
