package com.saha.composewebviewapptemplate.screens

// ============================================================================
// ONBOARDING SCREEN - MODERN AND PROFESSIONAL TEMPLATE
// ============================================================================
// This file contains a complete onboarding screen implementation with:
// - 3 customizable onboarding pages
// - Smooth animations and transitions
// - Modern gradient background design
// - Safe area handling for all devices
// - Easy customization for different brands
// ============================================================================

// Animation imports for smooth page transitions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

// Layout and UI imports
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

// Material Design 3 components
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

// Compose runtime and state management
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// UI styling and positioning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saha.composewebviewapptemplate.R

// ============================================================================
// DATA CLASSES
// ============================================================================

/**
 * Data class representing a single onboarding page
 * 
 * @param title Main heading text for the page
 * @param subtitle Secondary heading text (smaller than title)
 * @param description Detailed description text explaining the feature
 * @param icon Drawable resource ID for the page icon (recommended: 60dp size)
 */
data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: Int
)

// ============================================================================
// MAIN COMPOSABLE FUNCTION
// ============================================================================

/**
 * OnboardingScreen - A modern, animated onboarding experience
 * 
 * Features:
 * - 3 customizable pages with smooth transitions
 * - Gradient background (easily customizable)
 * - Page indicators showing current position
 * - Previous/Next navigation buttons
 * - Skip option for quick access
 * - Safe area handling for all device types
 * - Professional animations and micro-interactions
 * 
 * @param onFinish Callback function called when user completes onboarding
 *                  or clicks skip button
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    // ========================================================================
    // CUSTOMIZATION SECTION - EASILY MODIFY THESE VALUES
    // ========================================================================
    
    // Background gradient colors - Change these to match your brand
    // Current: Blue to purple gradient
    val gradientColors = listOf(
        Color(0xFF667eea), // Light blue
        Color(0xFF764ba2)  // Purple
    )
    
    // Alternative gradient options (uncomment to use):
    // Green gradient: Color(0xFF56ab2f), Color(0xFFa8e6cf)
    // Orange gradient: Color(0xFFff9a9e), Color(0xFFfecfef)
    // Dark gradient: Color(0xFF2c3e50), Color(0xFF34495e)

    // ========================================================================
    // ONBOARDING PAGES CONFIGURATION
    // ========================================================================
    // Add or remove pages by modifying this list
    // Each page requires: title, subtitle, description, and icon
    val pages = listOf(
        OnboardingPage(
            title = stringResource(R.string.onboarding_welcome_title),
            subtitle = stringResource(R.string.onboarding_welcome_subtitle),
            description = stringResource(R.string.onboarding_welcome_description),
            icon = R.drawable.my_app_logo // Replace with your app logo
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_features_title),
            subtitle = stringResource(R.string.onboarding_features_subtitle),
            description = stringResource(R.string.onboarding_features_description),
            icon = R.drawable.my_app_logo // Replace with feature icon
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_ready_title),
            subtitle = stringResource(R.string.onboarding_ready_subtitle),
            description = stringResource(R.string.onboarding_ready_description),
            icon = R.drawable.my_app_logo // Replace with ready icon
        )
    )

    // ========================================================================
    // STATE MANAGEMENT
    // ========================================================================
    
    // Current page index (0-based)
    var currentPage by remember { mutableStateOf(0) }
    
    // Animation visibility state for smooth page transitions
    var isVisible by remember { mutableStateOf(true) }

    // ========================================================================
    // PAGE TRANSITION ANIMATION
    // ========================================================================
    // This creates a smooth fade-out/fade-in effect when changing pages
    LaunchedEffect(currentPage) {
        isVisible = false
        kotlinx.coroutines.delay(100) // Brief pause for smooth transition
        isVisible = true
    }

    // ========================================================================
    // MAIN UI LAYOUT
    // ========================================================================
    
    // Full-screen container with gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors
                )
            )
    ) {
        // Main content column with safe area padding
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)    // Top safe area
                .windowInsetsPadding(WindowInsets.navigationBars) // Bottom safe area
                .padding(24.dp), // Additional padding for content
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // ================================================================
            // TOP SECTION - SKIP BUTTON
            // ================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Only show skip button on first two pages
                if (currentPage < pages.size - 1) {
                    TextButton(
                        onClick = onFinish
                    ) {
                        Text(
                            text = stringResource(R.string.skip),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ================================================================
            // MIDDLE SECTION - PAGE CONTENT WITH ANIMATIONS
            // ================================================================
            AnimatedVisibility(
                visible = isVisible,
                // Slide in from right with fade effect
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(600)
                ) + fadeIn(animationSpec = tween(600)),
                // Slide out to left with fade effect
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // ========================================================
                    // ICON CARD
                    // ========================================================
                    Card(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.2f) // Semi-transparent white
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = pages[currentPage].icon),
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // ========================================================
                    // TITLE TEXT
                    // ========================================================
                    Text(
                        text = pages[currentPage].title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ========================================================
                    // SUBTITLE TEXT
                    // ========================================================
                    Text(
                        text = pages[currentPage].subtitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp
                        ),
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ========================================================
                    // DESCRIPTION TEXT
                    // ========================================================
                    Text(
                        text = pages[currentPage].description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // ================================================================
            // BOTTOM SECTION - NAVIGATION CONTROLS
            // ================================================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ========================================================
                // PAGE INDICATORS (DOTS)
                // ========================================================
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, _ ->
                        // Animate indicator opacity based on current page
                        val alpha by animateFloatAsState(
                            targetValue = if (index == currentPage) 1f else 0.3f,
                            animationSpec = tween(300),
                            label = "indicator_alpha"
                        )
                        
                        // Individual page indicator dot
                        Box(
                            modifier = Modifier
                                .size(if (index == currentPage) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = alpha))
                        )
                        
                        // Spacing between dots
                        if (index < pages.size - 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ========================================================
                // NAVIGATION BUTTONS
                // ========================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ====================================================
                    // PREVIOUS BUTTON
                    // ====================================================
                    // Only show on pages 2 and 3 (not on first page)
                    if (currentPage > 0) {
                        OutlinedButton(
                            onClick = { 
                                if (currentPage > 0) {
                                    currentPage--
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.5f),
                                        Color.White.copy(alpha = 0.3f)
                                    )
                                )
                            )
                        ) {
                            Text("Previous")
                        }
                    } else {
                        // Invisible spacer to maintain button alignment
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    // ====================================================
                    // NEXT/GET STARTED BUTTON
                    // ====================================================
                    Button(
                        onClick = {
                            if (currentPage < pages.size - 1) {
                                // Move to next page
                                currentPage++
                            } else {
                                // Complete onboarding
                                onFinish()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = gradientColors[0] // Use first gradient color for text
                        ),
                        shape = RoundedCornerShape(25.dp), // Rounded button
                        modifier = Modifier
                            .height(50.dp)
                            .width(140.dp)
                    ) {
                        Text(
                            text = if (currentPage < pages.size - 1) {
                                stringResource(R.string.next)
                            } else {
                                stringResource(R.string.get_started)
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// CUSTOMIZATION GUIDE FOR TEMPLATE BUYERS
// ============================================================================
/*
 * EASY CUSTOMIZATION OPTIONS:
 * 
 * 1. CHANGE COLORS:
 *    - Modify gradientColors list to match your brand
 *    - Update button colors and text colors
 * 
 * 2. ADD/REMOVE PAGES:
 *    - Add new OnboardingPage entries to the pages list
 *    - Update corresponding strings in strings.xml
 * 
 * 3. CHANGE ICONS:
 *    - Replace R.drawable.my_app_logo with your custom icons
 *    - Recommended icon size: 60dp
 *    - Use vector drawables for best quality
 * 
 * 4. MODIFY TEXT:
 *    - Update strings in res/values/strings.xml
 *    - Adjust font sizes and styles in Text components
 * 
 * 5. ADJUST ANIMATIONS:
 *    - Modify tween durations for faster/slower animations
 *    - Change slide directions or effects
 * 
 * 6. LAYOUT CHANGES:
 *    - Adjust spacing with Spacer heights
 *    - Modify button sizes and shapes
 *    - Change card elevation and colors
 */