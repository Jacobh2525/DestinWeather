package com.destinweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Preset locations for surf/weather with coordinates
val presetLocations = listOf(
    Location("Destin", "FL", "US", 30.3935, -86.4958),
    Location("Panama City Beach", "FL", "US", 30.1523, -85.6594),
    Location("Pensacola", "FL", "US", 30.4213, -87.2169),
    Location("Fort Walton Beach", "FL", "US", 30.4058, -86.6188),
    Location("Gulf Shores", "AL", "US", 30.2460, -87.7008),
    Location("Orange Beach", "AL", "US", 30.2944, -87.6297),
    Location("Myrtle Beach", "SC", "US", 33.6891, -78.8867),
    Location("Miami", "FL", "US", 25.7617, -80.1918),
    Location("Tampa", "FL", "US", 27.9506, -82.4572),
    Location("Jacksonville", "FL", "US", 30.3322, -81.6557),
    Location("Key West", "FL", "US", 24.5551, -81.7800),
    Location("Cocoa Beach", "FL", "US", 28.3200, -80.6076),
    Location("Santa Rosa Beach", "FL", "US", 30.3960, -86.1728),
    Location("Seaside", "FL", "US", 30.3152, -86.1394),
    Location("Alys Beach", "FL", "US", 30.2855, -86.1650)
)

data class Location(
    val city: String,
    val state: String,
    val country: String,
    val lat: Double,
    val lon: Double
) {
    val displayName: String get() = "$city, $state"
    val queryName: String get() = "$city,$country"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    currentLocation: String,
    onLocationSelected: (String, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val filteredLocations = if (searchQuery.isBlank()) {
        presetLocations
    } else {
        presetLocations.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.city.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1a1a2e),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Location",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search cities...", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF64B5F6),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF64B5F6)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Locations List
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLocations) { location ->
                    LocationItem(
                        location = location,
                        isSelected = location.displayName == currentLocation,
                        onClick = {
                            onLocationSelected(location.queryName, location.lat, location.lon)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LocationItem(
    location: Location,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color(0xFF64B5F6).copy(alpha = 0.2f)
                else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF64B5F6) else Color.Gray
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = location.city,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "${location.state}, ${location.country}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF64B5F6)
            )
        }
    }
}
