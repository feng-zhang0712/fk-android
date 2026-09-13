package com.fk.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fk.sample.catalog.SampleDestination
import com.fk.sample.catalog.SampleGroup
import com.fk.sample.core.background.BackgroundDemoScreen
import com.fk.sample.core.biometric.BiometricDemoScreen
import com.fk.sample.core.file.FileDemoScreen
import com.fk.sample.core.i18n.I18nDemoScreen
import com.fk.sample.core.logging.LoggingDemoScreen
import com.fk.sample.core.mapping.MappingDemoScreen
import com.fk.sample.core.network.NetworkDemoScreen
import com.fk.sample.core.notification.NotificationDemoScreen
import com.fk.sample.core.permissions.PermissionsDemoScreen
import com.fk.sample.core.pluggable.PluggableDemoScreen
import com.fk.sample.core.security.SecurityDemoScreen
import com.fk.sample.core.storage.StorageDemoScreen
import com.fk.sample.home.SampleGroupScreen
import com.fk.sample.home.SampleHomeScreen
import com.fk.ui.theme.FkTheme

/**
 * Sample app entry: module hub → group list → component demo.
 *
 * Extends [FragmentActivity] so Jetpack BiometricPrompt can host prompts.
 */
class MainActivity : FragmentActivity() {
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
        onOpenGroup = { group ->
          navController.navigate(group.route)
        },
      )
    }
    composable(
      route = "group/{groupId}",
      arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
    ) { entry ->
      val groupId = entry.arguments?.getString("groupId").orEmpty()
      val group = SampleGroup.entries.firstOrNull { it.name.equals(groupId, ignoreCase = true) }
        ?: SampleGroup.Core
      SampleGroupScreen(
        group = group,
        onBack = { navController.popBackStack() },
        onOpen = { destination ->
          navController.navigate(destination.route)
        },
      )
    }
    composable(SampleDestination.PLUGGABLE) {
      PluggableDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.NETWORK) {
      NetworkDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.STORAGE) {
      StorageDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.LOGGING) {
      LoggingDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.MAPPING) {
      MappingDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.SECURITY) {
      SecurityDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.I18N) {
      I18nDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.PERMISSIONS) {
      PermissionsDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.BIOMETRIC) {
      BiometricDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.BACKGROUND) {
      BackgroundDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.NOTIFICATION) {
      NotificationDemoScreen(onBack = { navController.popBackStack() })
    }
    composable(SampleDestination.FILE) {
      FileDemoScreen(onBack = { navController.popBackStack() })
    }
  }
}
