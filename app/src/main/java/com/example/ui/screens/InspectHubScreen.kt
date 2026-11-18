package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ChainSpecsUiState
import com.example.ui.viewmodel.ChainSpecsViewModel
import com.example.ui.viewmodel.TxInspectorUiState
import com.example.ui.viewmodel.TxInspectorViewModel

@Composable
fun InspectHubScreen(
    txState: TxInspectorUiState,
    txViewModel: TxInspectorViewModel,
    chainSpecsState: ChainSpecsUiState,
    chainSpecsViewModel: ChainSpecsViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("inspect_hub_tab_row")
        ) {
            SegmentedButton(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.testTag("inspect_tab_tx")
            ) {
                Text("Raw Tx Hex")
            }
            SegmentedButton(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("inspect_tab_chains")
            ) {
                Text("Chain Specs & Calc")
            }
        }

        if (selectedSubTab == 0) {
            TxInspectorScreen(
                uiState = txState,
                viewModel = txViewModel,
                modifier = Modifier.weight(1f)
            )
        } else {
            ChainSpecsScreen(
                uiState = chainSpecsState,
                viewModel = chainSpecsViewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
