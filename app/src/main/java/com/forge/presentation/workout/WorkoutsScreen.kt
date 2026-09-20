package com.forge.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.core.designsystem.theme.ForgeTheme

@Composable
fun WorkoutsScreen(
    modifier: Modifier = Modifier,
    onStartEmptyWorkout: () -> Unit = {},
    onOpenExerciseLibrary: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ForgeTheme.colors.background)
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "WORKOUTS",
            color = ForgeTheme.colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Routines & Training Sessions",
            color = ForgeTheme.colors.textSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onStartEmptyWorkout,
            colors = ButtonDefaults.buttonColors(containerColor = ForgeTheme.colors.primary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "+ QUICK START EMPTY WORKOUT",
                color = ForgeTheme.colors.background,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Exercise Library Access Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ForgeTheme.colors.surface)
                .border(1.dp, ForgeTheme.colors.divider, RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenExerciseLibrary)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "EXERCISE LIBRARY",
                        color = ForgeTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "876 movements, muscle filters & variations",
                        color = ForgeTheme.colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Text("EXPLORE →", color = ForgeTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "MY ROUTINES",
            color = ForgeTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sample Routine Card (Foundation shell)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ForgeTheme.colors.surface)
                .border(1.dp, ForgeTheme.colors.divider, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "PPL V1 — Push Hypertrophy",
                    color = ForgeTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "5 Exercises • 18 Sets • ~60 min",
                    color = ForgeTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(name = "Workouts Screen Preview")
@Composable
fun WorkoutsScreenPreview() {
    ForgeTheme {
        WorkoutsScreen()
    }
}
