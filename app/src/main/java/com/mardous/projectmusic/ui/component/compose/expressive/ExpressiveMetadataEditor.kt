package com.mardous.projectmusic.ui.component.compose.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mardous.projectmusic.data.local.database.metadata.InstrumentFamily
import com.mardous.projectmusic.data.local.database.metadata.TagCategory

/**
 * Types of metadata that can be edited using the [ExpressiveMetadataEditor].
 */
enum class MetadataEditorType {
    Mood, Tag, Instrument
}

/**
 * A modal bottom sheet for creating or editing music metadata (Moods, Tags, Instruments).
 * Designed with Material 3 Expressive guidelines, featuring generous spacing and bold typography.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveMetadataEditor(
    type: MetadataEditorType,
    onDismissRequest: () -> Unit,
    onSave: (name: String, description: String, emoji: String, color: Int?, category: TagCategory?, family: InstrumentFamily?) -> Unit,
    modifier: Modifier = Modifier,
    existingNames: List<String> = emptyList(),
    initialName: String = "",
    initialDescription: String = "",
    initialEmoji: String = "🎵",
    initialColor: Int? = null,
    initialCategory: TagCategory = TagCategory.Activity,
    initialFamily: InstrumentFamily = InstrumentFamily.Strings,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var selectedEmoji by remember { mutableStateOf(initialEmoji) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedFamily by remember { mutableStateOf(initialFamily) }

    val isNameDuplicate = name.trim().isNotEmpty() &&
            name.trim().lowercase() != initialName.lowercase() &&
            existingNames.any { it.trim().lowercase() == name.trim().lowercase() }
    val isNameEmpty = name.trim().isEmpty()
    val canSave = !isNameEmpty && !isNameDuplicate

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge, // Expressive irregular corners
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp) // Generous spacing
        ) {
            // Header
            Text(
                text = when (type) {
                    MetadataEditorType.Mood -> if (initialName.isEmpty()) "New Mood" else "Edit Mood"
                    MetadataEditorType.Tag -> if (initialName.isEmpty()) "New Tag" else "Edit Tag"
                    MetadataEditorType.Instrument -> if (initialName.isEmpty()) "New Instrument" else "Edit Instrument"
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black, // Expressive bold weight
                color = MaterialTheme.colorScheme.onSurface
            )

            // Name Field with Validation
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = isNameEmpty || isNameDuplicate,
                supportingText = {
                    if (isNameEmpty) {
                        Text("Name is required")
                    } else if (isNameDuplicate) {
                        Text("A ${type.name.lowercase()} with this name already exists")
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                textStyle = MaterialTheme.typography.bodyLarge
            )

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = MaterialTheme.shapes.large,
                placeholder = { Text("What makes this ${type.name.lowercase()} unique?") }
            )

            // Contextual Dropdowns
            when (type) {
                MetadataEditorType.Tag -> {
                    CategoryDropdown(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }
                MetadataEditorType.Instrument -> {
                    FamilyDropdown(
                        selectedFamily = selectedFamily,
                        onFamilySelected = { selectedFamily = it }
                    )
                }
                MetadataEditorType.Mood -> {}
            }

            // Emoji/Icon Picker
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Identity Emoji",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                EmojiPickerRow(
                    selectedEmoji = selectedEmoji,
                    onEmojiSelected = { selectedEmoji = it }
                )
            }

            // Color Picker
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Tonal Accent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                ColorPickerRow(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        if (canSave) {
                            onSave(name, description, selectedEmoji, selectedColor, selectedCategory, selectedFamily)
                        }
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .padding(bottom = 4.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(horizontal = 32.dp)
                ) {
                    Text(
                        "Save ${type.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiPickerRow(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit
) {
    val emojis = listOf("🎵", "🎶", "🎧", "🎸", "🎹", "🎷", "🎻", "🎺", "🥁", "🎙️", "📻", "🎼", "🎤", "🎷", "🎸", "🎹", "✨", "🔥", "🌈", "🌊")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(emojis) { emoji ->
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .border(
                        width = 2.dp,
                        color = if (selectedEmoji == emoji) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = MaterialTheme.shapes.large
                    )
                    .clickable { onEmojiSelected(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 32.sp)
                if (selectedEmoji == emoji) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPickerRow(
    selectedColor: Int?,
    onColorSelected: (Int?) -> Unit
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        Color(0xFF6750A4), // Royal Purple
        Color(0xFF006A6A), // Teal
        Color(0xFF914F2B), // Earth Tonal
        Color(0xFF2E6B12), // Forest
        Color(0xFFBF360C), // Deep Spice
        Color(0xFF006064)  // Midnight Cyan
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 2.dp,
                        color = if (selectedColor == null) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(null) },
                contentAlignment = Alignment.Center
            ) {
                if (selectedColor == null) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        items(colors) { color ->
            val colorInt = color.toArgb()
            val isSelected = selectedColor == colorInt
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = 3.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorInt) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: TagCategory,
    onCategorySelected: (TagCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCategory.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TagCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyDropdown(
    selectedFamily: InstrumentFamily,
    onFamilySelected: (InstrumentFamily) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedFamily.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Instrument Family") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            InstrumentFamily.entries.forEach { family ->
                DropdownMenuItem(
                    text = { Text(family.name, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onFamilySelected(family)
                        expanded = false
                    }
                )
            }
        }
    }
}
