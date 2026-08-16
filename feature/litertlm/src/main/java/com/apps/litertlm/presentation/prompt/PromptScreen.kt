package com.apps.litertlm.presentation.prompt

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun PromptScreen(modifier: Modifier) {
    val viewModel: PromptViewModel = hiltViewModel()
}