package com.perqa.byebox

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.perqa.byebox.ui.main.MainScreen
import com.perqa.byebox.ui.main.MainScreenViewModel

@Composable
fun MainNavigation(viewModel: MainScreenViewModel) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            onItemClick = { navKey -> backStack.add(navKey) },
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}


