package com.taquangkhoi.napkincollect.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.taquangkhoi.napkincollect.R
import com.taquangkhoi.napkincollect.ui.theme.NapkinCollectTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var thought by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.napkin_logo_analogue_black),
                        contentDescription = "Logo",
                        modifier = Modifier.height(32.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { /* Handle settings icon click */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)
        ) {
            OutlinedTextField(
                value = thought,
                onValueChange = { thought = it },
                label = { Text("Enter your thought") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            OutlinedTextField(
                value = source,
                onValueChange = { source = it },
                label = { Text("Source of Thought") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { /* Handle send button click */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(text = "Send")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NapkinCollectTheme {
        MainScreen()
    }
}