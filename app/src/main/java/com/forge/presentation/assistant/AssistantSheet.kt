package com.forge.presentation.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.component.ForgeButton
import com.forge.core.designsystem.component.ForgeButtonSize
import com.forge.core.designsystem.component.ForgeButtonVariant
import com.forge.core.designsystem.component.ForgeGlassSurface
import com.forge.core.designsystem.component.ForgeGlassVariant
import com.forge.core.designsystem.theme.ForgeColors
import com.forge.core.designsystem.theme.ForgeTheme
import com.forge.domain.engine.AssistantMessage
import com.forge.domain.engine.AssistantService
import com.forge.domain.engine.EstimatedMeal
import com.forge.domain.engine.PendingAction
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssistantSheet(
    assistantService: AssistantService,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<AssistantMessage>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(
                AssistantMessage.Assistant(
                    text = "I am connected to your live FORGE state. You can ask about your training schedule, past workouts, exercise history, PRs, nutrition targets, or daily steps."
                )
            )
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(AssistantMessage.User(text))
        isProcessing = true
        inputText = ""

        scope.launch {
            val response = assistantService.processUserMessage(text)
            messages.add(response)
            isProcessing = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ForgeTheme.colors.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ForgeColors.CyanPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ForgeColors.CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "FORGE ASSISTANT",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ForgeTheme.colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Prompt Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val chips = listOf(
                    "What's my workout today?",
                    "How much protein is left?",
                    "What did I bench last time?",
                    "Move Friday to Saturday",
                    "How many steps today?"
                )
                chips.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ForgeColors.GlassFillSubtle)
                            .border(1.dp, ForgeColors.GlassStroke, RoundedCornerShape(12.dp))
                            .clickable { sendMessage(chip) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(chip, color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Conversation Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    when (msg) {
                        is AssistantMessage.User -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(ForgeColors.CyanPrimary)
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(msg.text, color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        is AssistantMessage.Assistant -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ForgeGlassSurface(
                                    variant = ForgeGlassVariant.SUBTLE,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(msg.text, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)

                                        // Pending Action Change Preview Card
                                        msg.pendingAction?.let { action ->
                                            Spacer(modifier = Modifier.height(12.dp))
                                            ForgeGlassSurface(
                                                variant = ForgeGlassVariant.STRONG,
                                                borderColor = ForgeColors.CyanPrimary,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = action.previewTitle,
                                                        color = ForgeColors.CyanPrimary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 1.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    action.diffRemove?.let { rem ->
                                                        Text(text = "− $rem", color = ForgeColors.CrimsonAlert, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    action.diffAdd?.let { add ->
                                                        Text(text = "+ $add", color = ForgeColors.CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        ForgeButton(
                                                            onClick = {
                                                                scope.launch {
                                                                    val res = assistantService.executeConfirmedAction(action)
                                                                    messages.add(AssistantMessage.Assistant(text = res))
                                                                    listState.animateScrollToItem(messages.size - 1)
                                                                }
                                                            },
                                                            text = "Confirm",
                                                            variant = ForgeButtonVariant.ACCENT,
                                                            size = ForgeButtonSize.SM
                                                        )
                                                        ForgeButton(
                                                            onClick = {
                                                                messages.add(AssistantMessage.Assistant(text = "Action cancelled."))
                                                            },
                                                            text = "Cancel",
                                                            variant = ForgeButtonVariant.SUBTLE,
                                                            size = ForgeButtonSize.SM
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Meal Estimation Card
                                        msg.estimatedMeal?.let { meal ->
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                ForgeButton(
                                                    onClick = {
                                                        messages.add(AssistantMessage.Assistant(text = "Logged ${meal.title} (${meal.totalCalories} kcal) to today's nutrition."))
                                                    },
                                                    text = "Add to Today",
                                                    variant = ForgeButtonVariant.PRIMARY,
                                                    size = ForgeButtonSize.SM
                                                )
                                                ForgeButton(
                                                    onClick = {
                                                        messages.add(AssistantMessage.Assistant(text = "Meal estimate discarded."))
                                                    },
                                                    text = "Discard",
                                                    variant = ForgeButtonVariant.SUBTLE,
                                                    size = ForgeButtonSize.SM
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = ForgeColors.CyanPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Querying FORGE state...", color = ForgeTheme.colors.textSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input Bar with Food Camera Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val meal = assistantService.parseFoodPhoto(300)
                        messages.add(AssistantMessage.User("Photo captured: Lunch plate"))
                        messages.add(
                            AssistantMessage.Assistant(
                                text = "Estimated meal from photo:\n${meal.items.joinToString("\n") { "• ${it.name}: ~${it.grams}g (~${it.calories} kcal)" }}\n\nTotal: ~${meal.totalCalories} kcal (${meal.totalProteinG}g Protein).",
                                estimatedMeal = meal
                            )
                        )
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ForgeColors.GlassFillSubtle)
                ) {
                    Icon(Icons.Default.CameraAlt, "Food Photo", tint = ForgeColors.CyanPrimary, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about workouts, macros, steps...", color = ForgeTheme.colors.textTertiary, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForgeColors.CyanPrimary,
                        unfocusedBorderColor = ForgeColors.GlassStroke,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { sendMessage(inputText) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ForgeColors.CyanPrimary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
