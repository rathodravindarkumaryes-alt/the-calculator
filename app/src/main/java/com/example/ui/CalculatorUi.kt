package com.example.ui

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalculationHistory
import com.example.viewmodel.CalculatorViewModel
import com.example.util.CurrencyService
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorAppContent(viewModel: CalculatorViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isWideScreen = screenWidth >= 600.dp

    // Atmospheric dynamic gradient based on theme
    val backgroundBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A), // Deep Slate
                Color(0xFF1E1B4B)  // Dark Indigo/Violet
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FAFC), // Off white
                Color(0xFFEFF6FF)  // Soft Blue-White
            )
        )
    }

    Scaffold(
        bottomBar = {
            if (!isWideScreen) {
                CalculatorBottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.navigateTo(it) 
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            if (isWideScreen) {
                CalculatorNavigationRail(
                    currentScreen = currentScreen,
                    onNavigate = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.navigateTo(it) 
                    }
                )
                VerticalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    label = "ScreenTransition"
                ) { targetState ->
                    when (targetState) {
                        "Basic" -> BasicCalculatorScreen(viewModel)
                        "Scientific" -> ScientificCalculatorScreen(viewModel)
                        "Age" -> AgeCalculatorScreen(viewModel)
                        "BMI" -> BmiCalculatorScreen(viewModel)
                        "Currency" -> CurrencyCalculatorScreen(viewModel)
                        "EMI" -> EmiCalculatorScreen(viewModel)
                        "GST" -> GstCalculatorScreen(viewModel)
                        "Percentage" -> PercentageCalculatorScreen(viewModel)
                        "History" -> HistoryScreen(viewModel, isFavoriteOnly = false)
                        "Favorites" -> HistoryScreen(viewModel, isFavoriteOnly = true)
                        "Settings" -> SettingsScreen(viewModel)
                        else -> DashboardScreen(viewModel)
                    }
                }
            }
        }
    }
}

// --- NAVIGATION ---
@Composable
fun CalculatorBottomNavBar(currentScreen: String, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = Color.Transparent,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        NavigationBarItem(
            selected = currentScreen in listOf("Dashboard", "Basic", "Scientific", "Age", "BMI", "Currency", "EMI", "GST", "Percentage"),
            onClick = { onNavigate("Dashboard") },
            icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculators") },
            label = { Text("Calculators", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_dashboard")
        )
        NavigationBarItem(
            selected = currentScreen == "History",
            onClick = { onNavigate("History") },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_history")
        )
        NavigationBarItem(
            selected = currentScreen == "Favorites",
            onClick = { onNavigate("Favorites") },
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_favorites")
        )
        NavigationBarItem(
            selected = currentScreen == "Settings",
            onClick = { onNavigate("Settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp) },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}

@Composable
fun CalculatorNavigationRail(currentScreen: String, onNavigate: (String) -> Unit) {
    NavigationRail(
        containerColor = Color.Transparent,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        NavigationRailItem(
            selected = currentScreen in listOf("Dashboard", "Basic", "Scientific", "Age", "BMI", "Currency", "EMI", "GST", "Percentage"),
            onClick = { onNavigate("Dashboard") },
            icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculators") },
            label = { Text("Calculators") }
        )
        NavigationRailItem(
            selected = currentScreen == "History",
            onClick = { onNavigate("History") },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History") }
        )
        NavigationRailItem(
            selected = currentScreen == "Favorites",
            onClick = { onNavigate("Favorites") },
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites") }
        )
        NavigationRailItem(
            selected = currentScreen == "Settings",
            onClick = { onNavigate("Settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

// --- 1. DASHBOARD SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: CalculatorViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val configuration = LocalConfiguration.current
    val columns = if (configuration.screenWidthDp >= 600) 4 else 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "the calculater",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "A premium suite of exact calculators",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme"
                )
            }
        }

        // Feature Dashboard Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                DashboardCard(
                    title = "Basic",
                    desc = "Standard Arithmetic",
                    icon = Icons.Default.Dialpad,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.navigateTo("Basic") }
                )
            }
            item {
                DashboardCard(
                    title = "Scientific",
                    desc = "Advanced Math & Trig",
                    icon = Icons.Default.Functions,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { viewModel.navigateTo("Scientific") }
                )
            }
            item {
                DashboardCard(
                    title = "Age",
                    desc = "Birthday Countdown",
                    icon = Icons.Default.Cake,
                    color = Color(0xFFEC4899),
                    onClick = { viewModel.navigateTo("Age") }
                )
            }
            item {
                DashboardCard(
                    title = "BMI",
                    desc = "Weight Health Index",
                    icon = Icons.Default.MonitorWeight,
                    color = Color(0xFF10B981),
                    onClick = { viewModel.navigateTo("BMI") }
                )
            }
            item {
                DashboardCard(
                    title = "Currency",
                    desc = "Real-time Converter",
                    icon = Icons.Default.CurrencyExchange,
                    color = Color(0xFFF59E0B),
                    onClick = { viewModel.navigateTo("Currency") }
                )
            }
            item {
                DashboardCard(
                    title = "EMI",
                    desc = "Loan Instalments",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = Color(0xFF8B5CF6),
                    onClick = { viewModel.navigateTo("EMI") }
                )
            }
            item {
                DashboardCard(
                    title = "GST",
                    desc = "Tax Calculator",
                    icon = Icons.Default.ReceiptLong,
                    color = Color(0xFF06B6D4),
                    onClick = { viewModel.navigateTo("GST") }
                )
            }
            item {
                DashboardCard(
                    title = "Percentages",
                    desc = "Discount & Profit-Loss",
                    icon = Icons.Default.Percent,
                    color = Color(0xFFEF4444),
                    onClick = { viewModel.navigateTo("Percentage") }
                )
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
            .testTag("dashboard_card_$title"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- SUB SCREEN CONTAINER HEADER ---
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

// --- 2. BASIC CALCULATOR ---
@Composable
fun BasicCalculatorScreen(viewModel: CalculatorViewModel) {
    val expression by viewModel.basicExpression.collectAsState()
    val result by viewModel.basicResult.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
    ) {
        ScreenHeader(
            title = "Basic Calculator",
            onBack = { viewModel.navigateTo("Dashboard") }
        ) {
            IconButton(onClick = {
                if (result.isNotEmpty()) {
                    copyToClipboard(context, result)
                }
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy result")
            }
            IconButton(onClick = {
                if (result.isNotEmpty()) {
                    shareText(context, "$expression = $result")
                }
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share result")
            }
        }

        // Display panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expression.ifEmpty { "0" },
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.End,
                modifier = Modifier.testTag("basic_expr_display")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.testTag("basic_result_display")
            )
        }

        // Keyboard grid
        val buttons = listOf(
            listOf("C", "()", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "⌫", "=")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .padding(12.dp)
        ) {
            for (row in buttons) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (btn in row) {
                        val haptic = LocalHapticFeedback.current
                        val isOperator = btn in listOf("÷", "×", "-", "+", "=")
                        val isSpecial = btn in listOf("C", "()", "%", "⌫")
                        val containerColor = when {
                            btn == "=" -> MaterialTheme.colorScheme.primary
                            isOperator -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            isSpecial -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        }
                        val contentColor = when {
                            btn == "=" -> MaterialTheme.colorScheme.onPrimary
                            isOperator -> MaterialTheme.colorScheme.secondary
                            isSpecial -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onBasicInput(btn)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .testTag("btn_$btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = btn,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 3. SCIENTIFIC CALCULATOR ---
@Composable
fun ScientificCalculatorScreen(viewModel: CalculatorViewModel) {
    val expression by viewModel.scientificExpression.collectAsState()
    val result by viewModel.scientificResult.collectAsState()
    val useRadians by viewModel.scientificUseRadians.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
    ) {
        ScreenHeader(
            title = "Scientific Calculator",
            onBack = { viewModel.navigateTo("Dashboard") }
        ) {
            TextButton(onClick = { viewModel.toggleRadians() }) {
                Text(if (useRadians) "RAD" else "DEG", fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = {
                if (result.isNotEmpty()) {
                    copyToClipboard(context, result)
                }
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy result")
            }
            IconButton(onClick = {
                if (result.isNotEmpty()) {
                    shareText(context, "$expression = $result")
                }
            }) {
                Icon(Icons.Default.Share, contentDescription = "Share result")
            }
        }

        // Display panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expression.ifEmpty { "0" },
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.End,
                modifier = Modifier.testTag("sci_expr_display")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.End,
                modifier = Modifier.testTag("sci_result_display")
            )
        }

        // Grid buttons
        val sciButtons = listOf(
            listOf("sin", "cos", "tan", "^", "pi"),
            listOf("log", "ln", "sqrt", "!", "e"),
            listOf("C", "()", "%", "÷", "⌫"),
            listOf("7", "8", "9", "×", "-"),
            listOf("4", "5", "6", "+", "="),
            listOf("1", "2", "3", "0", ".")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .padding(8.dp)
        ) {
            for (row in sciButtons) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (btn in row) {
                        val haptic = LocalHapticFeedback.current
                        val isSci = btn in listOf("sin", "cos", "tan", "log", "ln", "sqrt", "^", "!", "pi", "e")
                        val isOperator = btn in listOf("÷", "×", "-", "+", "=")
                        val isSpecial = btn in listOf("C", "()", "%", "⌫")
                        
                        val containerColor = when {
                            btn == "=" -> MaterialTheme.colorScheme.secondary
                            isSci -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            isOperator -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            isSpecial -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        }
                        val contentColor = when {
                            btn == "=" -> MaterialTheme.colorScheme.onSecondary
                            isSci -> MaterialTheme.colorScheme.primary
                            isOperator -> MaterialTheme.colorScheme.secondary
                            isSpecial -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onScientificInput(btn)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .testTag("btn_sci_$btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = btn,
                                fontSize = if (isSci && btn.length > 3) 12.sp else 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 4. AGE CALCULATOR ---
@Composable
fun AgeCalculatorScreen(viewModel: CalculatorViewModel) {
    val birthDate by viewModel.ageBirthDate.collectAsState()
    val targetDate by viewModel.ageTargetDate.collectAsState()
    val result by viewModel.ageResult.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(birthDate, targetDate) {
        viewModel.calculateAge()
    }

    val birthDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            viewModel.updateBirthDate(LocalDate.of(year, month + 1, day))
        },
        birthDate.year, birthDate.monthValue - 1, birthDate.dayOfMonth
    )

    val targetDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            viewModel.updateTargetDate(LocalDate.of(year, month + 1, day))
        },
        targetDate.year, targetDate.monthValue - 1, targetDate.dayOfMonth
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ScreenHeader(
                title = "Age Calculator",
                onBack = { viewModel.navigateTo("Dashboard") }
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Birth Date Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                birthDialog.show()
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Date of Birth", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(birthDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select DOB", tint = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider()

                    // Target Date Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                targetDialog.show()
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Target Date (Today)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(targetDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Target Date", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (result != null) {
            val res = result!!
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL AGE", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AgeMetricBox(valStr = res.years.toString(), label = "Years")
                            AgeMetricBox(valStr = res.months.toString(), label = "Months")
                            AgeMetricBox(valStr = res.days.toString(), label = "Days")
                        }
                    }
                }
            }

            item {
                // Countdown next birthday card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEC4899).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.Cake, contentDescription = "Birthday Countdown", tint = Color(0xFFEC4899))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Next Birthday", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "In ${res.nextBirthdayMonths} months & ${res.nextBirthdayDays} days",
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Falls on a ${res.nextBirthdayDayOfWeek}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            item {
                // Detailed total counts
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Detailed Statistics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        StatRow("Total Months", "${res.totalMonths} months")
                        StatRow("Total Weeks", "${res.totalWeeks} weeks")
                        StatRow("Total Days", "${res.totalDays} days")
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Please select a DOB earlier than target date",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AgeMetricBox(valStr: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .width(60.dp)
    ) {
        Text(valStr, fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

// --- 5. BMI CALCULATOR ---
@Composable
fun BmiCalculatorScreen(viewModel: CalculatorViewModel) {
    val mode by viewModel.bmiMode.collectAsState()
    val weightMetric by viewModel.bmiWeightMetric.collectAsState()
    val heightMetric by viewModel.bmiHeightMetric.collectAsState()
    val weightImperial by viewModel.bmiWeightImperial.collectAsState()
    val heightFeet by viewModel.bmiHeightFeet.collectAsState()
    val heightInches by viewModel.bmiHeightInches.collectAsState()
    val result by viewModel.bmiResult.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(mode, weightMetric, heightMetric, weightImperial, heightFeet, heightInches) {
        viewModel.calculateBmi()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ScreenHeader(
                title = "BMI Calculator",
                onBack = { viewModel.navigateTo("Dashboard") }
            )
        }

        item {
            // Segmented mode selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = mode == "Metric",
                    onClick = { viewModel.updateBmiMode("Metric") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Metric (kg/cm)")
                }
                SegmentedButton(
                    selected = mode == "Imperial",
                    onClick = { viewModel.updateBmiMode("Imperial") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Imperial (lbs/ft)")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (mode == "Metric") {
                        // Weight metric
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Weight", fontWeight = FontWeight.Bold)
                                Text("$weightMetric kg", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = weightMetric.toFloatOrNull() ?: 70f,
                                onValueChange = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onBmiValueChange("weight_metric", it.roundToInt().toString()) 
                                },
                                valueRange = 20f..200f
                            )
                        }

                        // Height metric
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Height", fontWeight = FontWeight.Bold)
                                Text("$heightMetric cm", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = heightMetric.toFloatOrNull() ?: 170f,
                                onValueChange = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onBmiValueChange("height_metric", it.roundToInt().toString()) 
                                },
                                valueRange = 100f..250f
                            )
                        }
                    } else {
                        // Weight imperial
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Weight", fontWeight = FontWeight.Bold)
                                Text("$weightImperial lbs", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = weightImperial.toFloatOrNull() ?: 150f,
                                onValueChange = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onBmiValueChange("weight_imperial", it.roundToInt().toString()) 
                                },
                                valueRange = 50f..400f
                            )
                        }

                        // Height feet & inches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Feet", fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = heightFeet,
                                    onValueChange = { viewModel.onBmiValueChange("height_feet", it) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Inches", fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = heightInches,
                                    onValueChange = { viewModel.onBmiValueChange("height_inches", it) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        if (result != null) {
            val res = result!!
            val barColor = when (res.status) {
                "Underweight" -> Color(0xFF3B82F6)
                "Normal" -> Color(0xFF10B981)
                "Overweight" -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = barColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("BMI SCORE", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.1f", res.bmi),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = barColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = res.status.uppercase(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Healthy Guidelines", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "A healthy BMI range is between 18.5 and 24.9.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Based on your height, your ideal healthy weight is:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${String.format("%.1f", res.minHealthyWeight)} ${res.unit} - ${String.format("%.1f", res.maxHealthyWeight)} ${res.unit}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// --- 6. CURRENCY CONVERTER ---
@Composable
fun CurrencyCalculatorScreen(viewModel: CalculatorViewModel) {
    val base by viewModel.currencyBase.collectAsState()
    val target by viewModel.currencyTarget.collectAsState()
    val amount by viewModel.currencyAmount.collectAsState()
    val result by viewModel.currencyResult.collectAsState()
    val loading by viewModel.currencyLoading.collectAsState()
    val rates by viewModel.currencyRates.collectAsState()
    val haptic = LocalHapticFeedback.current

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ScreenHeader(
                title = "Currency Converter",
                onBack = { viewModel.navigateTo("Dashboard") }
            ) {
                IconButton(onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.syncCurrencyRates() 
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh rates",
                        tint = if (loading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Currency Selection Dropdowns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("From", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            CurrencyDropdown(
                                selected = base,
                                onSelected = { viewModel.updateCurrencyBase(it) }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.Bottom)
                                .padding(bottom = 12.dp)
                        ) {
                            IconButton(onClick = {
                                val temp = base
                                viewModel.updateCurrencyBase(target)
                                viewModel.updateCurrencyTarget(temp)
                            }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap")
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("To", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            CurrencyDropdown(
                                selected = target,
                                onSelected = { viewModel.updateCurrencyTarget(it) }
                            )
                        }
                    }

                    // Input Amount Field
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { viewModel.updateCurrencyAmount(it) },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CONVERTED VALUE", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$result $target",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1 $base = ${String.format("%.4f", (rates[target] ?: 1.0) / (rates[base] ?: 1.0))} $target",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveCurrencyToHistory()
                        Toast.makeText(context, "Saved to History!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to History")
                }
                OutlinedButton(
                    onClick = {
                        shareText(context, "$amount $base is $result $target")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Result")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selected,
            onValueChange = {},
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CurrencyService.SUPPORTED_CURRENCIES.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- 7. EMI CALCULATOR ---
@Composable
fun EmiCalculatorScreen(viewModel: CalculatorViewModel) {
    val principal by viewModel.emiPrincipal.collectAsState()
    val rate by viewModel.emiRate.collectAsState()
    val tenure by viewModel.emiTenure.collectAsState()
    val tenureType by viewModel.emiTenureType.collectAsState()
    val result by viewModel.emiResult.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(principal, rate, tenure, tenureType) {
        viewModel.calculateEmi()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ScreenHeader(
                title = "EMI Calculator",
                onBack = { viewModel.navigateTo("Dashboard") }
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Loan Principal Amount
                    OutlinedTextField(
                        value = principal,
                        onValueChange = { viewModel.onEmiValueChange("principal", it) },
                        label = { Text("Loan Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Interest rate
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { viewModel.onEmiValueChange("rate", it) },
                        label = { Text("Interest Rate (% per annum)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tenure Length
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tenure,
                            onValueChange = { viewModel.onEmiValueChange("tenure", it) },
                            label = { Text("Tenure") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        // Selection Row
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                            SegmentedButton(
                                selected = tenureType == "Years",
                                onClick = { viewModel.updateEmiTenureType("Years") },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Yr")
                            }
                            SegmentedButton(
                                selected = tenureType == "Months",
                                onClick = { viewModel.updateEmiTenureType("Months") },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("Mo")
                            }
                        }
                    }
                }
            }
        }

        if (result != null) {
            val res = result!!
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("MONTHLY INSTALMENT (EMI)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%.2f", res.monthlyEmi)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Payment Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        StatRow("Principal Amount", "₹${principal}")
                        StatRow("Total Interest Payable", "₹${String.format("%.2f", res.totalInterest)}")
                        StatRow("Total Amount Payable", "₹${String.format("%.2f", res.totalPayment)}")

                        Spacer(modifier = Modifier.height(8.dp))
                        // Multi-color breakdown bar
                        val principalShare = (principal.toDoubleOrNull() ?: 1.0) / res.totalPayment
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(principalShare.toFloat())
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight((1f - principalShare).toFloat())
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Principal (${String.format("%.1f", principalShare * 100)}%)", fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Interest (${String.format("%.1f", (1 - principalShare) * 100)}%)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// --- 8. GST CALCULATOR ---
@Composable
fun GstCalculatorScreen(viewModel: CalculatorViewModel) {
    val amount by viewModel.gstAmount.collectAsState()
    val rate by viewModel.gstRate.collectAsState()
    val type by viewModel.gstType.collectAsState()
    val result by viewModel.gstResult.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(amount, rate, type) {
        viewModel.calculateGst()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ScreenHeader(
                title = "GST Calculator",
                onBack = { viewModel.navigateTo("Dashboard") }
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Tax calculation type selection
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = type == "Exclusive",
                            onClick = { viewModel.updateGstType("Exclusive") },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("GST Exclusive (+ Add)")
                        }
                        SegmentedButton(
                            selected = type == "Inclusive",
                            onClick = { viewModel.updateGstType("Inclusive") },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("GST Inclusive (- Subtract)")
                        }
                    }

                    // Original Base Amount Field
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { viewModel.onGstValueChange("amount", it) },
                        label = { Text("Base / Net Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Standard Tax Rate Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tax Rate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("5", "12", "18", "28").forEach { pct ->
                                val selected = rate == pct
                                InputChip(
                                    selected = selected,
                                    onClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.onGstValueChange("rate", pct) 
                                    },
                                    label = { Text("$pct%") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Custom GST rate input
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { viewModel.onGstValueChange("rate", it) },
                        label = { Text("Custom GST Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (result != null) {
            val res = result!!
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL BILL VALUE", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%.2f", res.totalAmount)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Summary Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        StatRow("Net Price (Base)", "₹${String.format("%.2f", res.netAmount)}")
                        StatRow("GST Amount Payable", "₹${String.format("%.2f", res.gstAmount)}")
                        StatRow("Final Invoice Total", "₹${String.format("%.2f", res.totalAmount)}")
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// --- 9. PERCENTAGE, DISCOUNT, PROFIT & LOSS ---
@Composable
fun PercentageCalculatorScreen(viewModel: CalculatorViewModel) {
    val tab by viewModel.percentageTab.collectAsState()
    val pctMode by viewModel.pctMode.collectAsState()
    val pctVal1 by viewModel.pctVal1.collectAsState()
    val pctVal2 by viewModel.pctVal2.collectAsState()
    val pctResult by viewModel.pctResult.collectAsState()

    val discPrice by viewModel.discPrice.collectAsState()
    val discPercent by viewModel.discPercent.collectAsState()
    val discTax by viewModel.discTax.collectAsState()
    val discResult by viewModel.discResult.collectAsState()

    val plCost by viewModel.plCostPrice.collectAsState()
    val plSelling by viewModel.plSellingPrice.collectAsState()
    val plResult by viewModel.plResult.collectAsState()

    // Real-time calculations
    LaunchedEffect(tab, pctMode, pctVal1, pctVal2, discPrice, discPercent, discTax, plCost, plSelling) {
        when (tab) {
            "Percentage" -> viewModel.calculatePercentage()
            "Discount" -> viewModel.calculateDiscount()
            "Profit-Loss" -> viewModel.calculateProfitLoss()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ScreenHeader(
                title = "Percentages & Business",
                onBack = { viewModel.navigateTo("Dashboard") }
            )
        }

        item {
            // Tab row
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                SegmentedButton(
                    selected = tab == "Percentage",
                    onClick = { viewModel.updatePercentageTab("Percentage") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Percentages")
                }
                SegmentedButton(
                    selected = tab == "Discount",
                    onClick = { viewModel.updatePercentageTab("Discount") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Discount")
                }
                SegmentedButton(
                    selected = tab == "Profit-Loss",
                    onClick = { viewModel.updatePercentageTab("Profit-Loss") },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) {
                    Text("Profit/Loss")
                }
            }
        }

        when (tab) {
            "Percentage" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Sub modes dropdown
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        when (pctMode) {
                                            "Mode1" -> "What is X% of Y?"
                                            "Mode2" -> "X is what % of Y?"
                                            else -> "Percentage Change (X to Y)"
                                        }
                                    )
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("What is X% of Y?") },
                                        onClick = { viewModel.updatePctMode("Mode1"); expanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("X is what % of Y?") },
                                        onClick = { viewModel.updatePctMode("Mode2"); expanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Percentage Change (X to Y)") },
                                        onClick = { viewModel.updatePctMode("Mode3"); expanded = false }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = pctVal1,
                                    onValueChange = { viewModel.onPctValueChange("val1", it) },
                                    label = { Text("Value X") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = pctVal2,
                                    onValueChange = { viewModel.onPctValueChange("val2", it) },
                                    label = { Text("Value Y") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RESULT", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(pctResult, fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            "Discount" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = discPrice,
                                onValueChange = { viewModel.onDiscValueChange("price", it) },
                                label = { Text("Original Price") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = discPercent,
                                    onValueChange = { viewModel.onDiscValueChange("percent", it) },
                                    label = { Text("Discount (%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = discTax,
                                    onValueChange = { viewModel.onDiscValueChange("tax", it) },
                                    label = { Text("Tax (%)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (discResult != null) {
                    val res = discResult!!
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("FINAL DISCOUNTED PRICE", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 2.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("₹${String.format("%.2f", res.finalPrice)}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Summary Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                StatRow("Original Base Price", "₹${discPrice}")
                                StatRow("Discount Savings Amount", "₹${String.format("%.2f", res.discountAmount)}")
                                StatRow("Added Tax Amount", "₹${String.format("%.2f", res.taxAmount)}")
                            }
                        }
                    }
                }
            }

            "Profit-Loss" -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = plCost,
                                onValueChange = { viewModel.onPlValueChange("cost", it) },
                                label = { Text("Cost Price (CP)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = plSelling,
                                onValueChange = { viewModel.onPlValueChange("selling", it) },
                                label = { Text("Selling Price (SP)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (plResult != null) {
                    val res = plResult!!
                    val barColor = when (res.status) {
                        "Profit" -> Color(0xFF10B981)
                        "Loss" -> Color(0xFFEF4444)
                        else -> Color(0xFF06B6D4)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = barColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(res.status.uppercase(), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 2.sp, color = barColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = (if (res.difference >= 0) "₹" else "-₹") + String.format("%.2f", kotlin.math.abs(res.difference)),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = barColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format("%.1f", res.pctDifference)}%",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = barColor
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// --- 10. HISTORY & FAVORITES SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: CalculatorViewModel, isFavoriteOnly: Boolean) {
    val history by viewModel.historyList.collectAsState()
    val favorites by viewModel.favoritesList.collectAsState()
    val displayList = if (isFavoriteOnly) favorites else history
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
    ) {
        ScreenHeader(
            title = if (isFavoriteOnly) "Favorites" else "Calculation History",
            onBack = { viewModel.navigateTo("Dashboard") }
        ) {
            if (!isFavoriteOnly && history.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearAllHistory() },
                    modifier = Modifier.testTag("clear_history_btn")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all")
                }
            }
        }

        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isFavoriteOnly) Icons.Default.FavoriteBorder else Icons.Default.HistoryToggleOff,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isFavoriteOnly) "No favorite calculations yet" else "No history recorded yet",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Perform calculations to record history!",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayList, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Load item back into calculator
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (item.type == "Basic") {
                                    viewModel.navigateTo("Basic")
                                    viewModel.onBasicInput("C")
                                    item.expression.forEach { ch ->
                                        viewModel.onBasicInput(ch.toString())
                                    }
                                } else if (item.type == "Scientific") {
                                    viewModel.navigateTo("Scientific")
                                    viewModel.onScientificInput("C")
                                    item.expression.forEach { ch ->
                                        viewModel.onScientificInput(ch.toString())
                                    }
                                }
                            }
                            .testTag("history_card_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = item.type,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (item.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteHistoryItem(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = item.expression,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.result,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 11. SETTINGS SCREEN ---
@Composable
fun SettingsScreen(viewModel: CalculatorViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
    ) {
        ScreenHeader(
            title = "Settings",
            onBack = { viewModel.navigateTo("Dashboard") }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Appearance & Theme", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme", fontWeight = FontWeight.Bold)
                            Text("Toggle dark or light mode palette", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("settings_theme_switch")
                        )
                    }
                }
            }

            item {
                Text("Local Databases", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Clear History Database", fontWeight = FontWeight.Bold)
                                Text("Permanently wipe your calculation history and favorites local records.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Button(
                                onClick = {
                                    viewModel.clearAllHistory()
                                    Toast.makeText(context, "All history cleared!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("settings_clear_db_btn")
                            ) {
                                Text("Wipe")
                            }
                        }
                    }
                }
            }

            item {
                Text("About The Calculater", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Version", fontWeight = FontWeight.Bold)
                        Text("1.0.0 (Premium Release)", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Built with", fontWeight = FontWeight.Bold)
                        Text("Kotlin, Jetpack Compose, Material Design 3, Room, and Coroutines.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

// --- SYSTEM UTILS ---
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Calculation Result", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard: $text", Toast.LENGTH_SHORT).show()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Result"))
}
