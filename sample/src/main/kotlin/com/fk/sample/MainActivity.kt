package com.fk.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fk.sample.catalog.SampleDestination
import com.fk.sample.core.pluggable.PluggableDemoScreen
import com.fk.sample.home.SampleHomeScreen
import com.fk.ui.theme.FkTheme

/**
 * Sample app entry: grouped catalog hub + per-component demo routes.
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      FkTheme {
        SampleNavHost()
      }
    }
  }
}

@Composable
private fun SampleNavHost() {
  val navController = rememberNavController()
  NavHost(
    navController = navController,
    startDestination = SampleDestination.HOME,
  ) {
    composable(SampleDestination.HOME) {
      SampleHomeScreen(
        onOpen = { destination ->
          navController.navigate(destination.route)
        },
      )
    }
    composable(SampleDestination.PLUGGABLE) {
      PluggableDemoScreen(
        onBack = { navController.popBackStack() },
      )
    }
  }
}
