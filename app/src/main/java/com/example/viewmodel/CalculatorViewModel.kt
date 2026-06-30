package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CalculationHistory
import com.example.data.CalculationRepository
import com.example.util.CurrencyService
import com.example.util.MathParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.roundToInt

class CalculatorViewModel(private val repository: CalculationRepository) : ViewModel() {

    // Theme state (true = dark theme)
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow("Basic")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // History and Favorites Flow from Database
    val historyList: StateFlow<List<CalculationHistory>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesList: StateFlow<List<CalculationHistory>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFavorite(item: CalculationHistory) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(item.id, !item.isFavorite)
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    // --- 1. Basic & Scientific Calculator State & Actions ---
    private val _basicExpression = MutableStateFlow("")
    val basicExpression: StateFlow<String> = _basicExpression.asStateFlow()

    private val _basicResult = MutableStateFlow("")
    val basicResult: StateFlow<String> = _basicResult.asStateFlow()

    private val _scientificExpression = MutableStateFlow("")
    val scientificExpression: StateFlow<String> = _scientificExpression.asStateFlow()

    private val _scientificResult = MutableStateFlow("")
    val scientificResult: StateFlow<String> = _scientificResult.asStateFlow()

    private val _scientificUseRadians = MutableStateFlow(true)
    val scientificUseRadians: StateFlow<Boolean> = _scientificUseRadians.asStateFlow()

    fun toggleRadians() {
        _scientificUseRadians.value = !_scientificUseRadians.value
    }

    fun onBasicInput(input: String) {
        when (input) {
            "C" -> {
                _basicExpression.value = ""
                _basicResult.value = ""
            }
            "⌫" -> {
                val current = _basicExpression.value
                if (current.isNotEmpty()) {
                    _basicExpression.value = current.dropLast(1)
                }
            }
            "=" -> {
                evaluateBasic()
            }
            "()" -> {
                val current = _basicExpression.value
                val openParentheses = current.count { it == '(' }
                val closeParentheses = current.count { it == ')' }
                val lastChar = current.lastOrNull()
                
                _basicExpression.value = if (lastChar == null || lastChar in listOf('+', '-', '×', '÷', '(')) {
                    current + "("
                } else if (openParentheses > closeParentheses && lastChar.isDigit() || lastChar == ')') {
                    current + ")"
                } else {
                    current + "("
                }
            }
            else -> {
                _basicExpression.value += input
            }
        }
    }

    fun onScientificInput(input: String) {
        when (input) {
            "C" -> {
                _scientificExpression.value = ""
                _scientificResult.value = ""
            }
            "⌫" -> {
                val current = _scientificExpression.value
                if (current.isNotEmpty()) {
                    _scientificExpression.value = current.dropLast(1)
                }
            }
            "=" -> {
                evaluateScientific()
            }
            "sin", "cos", "tan", "log", "ln", "sqrt" -> {
                _scientificExpression.value += "$input("
            }
            "()" -> {
                val current = _scientificExpression.value
                val openParentheses = current.count { it == '(' }
                val closeParentheses = current.count { it == ')' }
                val lastChar = current.lastOrNull()
                
                _scientificExpression.value = if (lastChar == null || lastChar in listOf('+', '-', '×', '÷', '(', '^')) {
                    current + "("
                } else if (openParentheses > closeParentheses && (lastChar.isDigit() || lastChar == ')' || lastChar == 'π' || lastChar == 'e')) {
                    current + ")"
                } else {
                    current + "("
                }
            }
            else -> {
                _scientificExpression.value += input
            }
        }
    }

    private fun evaluateBasic() {
        val expr = _basicExpression.value
        if (expr.isEmpty()) return
        try {
            val parser = MathParser(useRadians = true)
            val evalResult = parser.evaluate(expr)
            val formatted = formatResult(evalResult)
            _basicResult.value = formatted
            saveToHistory("Basic", expr, formatted)
        } catch (e: Exception) {
            _basicResult.value = "Error"
        }
    }

    private fun evaluateScientific() {
        val expr = _scientificExpression.value
        if (expr.isEmpty()) return
        try {
            val parser = MathParser(useRadians = _scientificUseRadians.value)
            val evalResult = parser.evaluate(expr)
            val formatted = formatResult(evalResult)
            _scientificResult.value = formatted
            saveToHistory("Scientific", expr, formatted)
        } catch (e: Exception) {
            _scientificResult.value = "Error"
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format("%.6f", value).trimEnd('0').trimEnd('.')
        }
    }

    private fun saveToHistory(type: String, expr: String, res: String) {
        viewModelScope.launch {
            repository.insertHistory(
                CalculationHistory(
                    type = type,
                    expression = expr,
                    result = res
                )
            )
        }
    }

    // --- 2. Age Calculator State & Actions ---
    private val _ageBirthDate = MutableStateFlow(LocalDate.now().minusYears(25))
    val ageBirthDate: StateFlow<LocalDate> = _ageBirthDate.asStateFlow()

    private val _ageTargetDate = MutableStateFlow(LocalDate.now())
    val ageTargetDate: StateFlow<LocalDate> = _ageTargetDate.asStateFlow()

    data class AgeResult(
        val years: Int,
        val months: Int,
        val days: Int,
        val totalMonths: Long,
        val totalWeeks: Long,
        val totalDays: Long,
        val daysUntilNextBirthday: Long,
        val nextBirthdayMonths: Int,
        val nextBirthdayDays: Int,
        val nextBirthdayDayOfWeek: String
    )

    private val _ageResult = MutableStateFlow<AgeResult?>(null)
    val ageResult: StateFlow<AgeResult?> = _ageResult.asStateFlow()

    fun updateBirthDate(date: LocalDate) {
        _ageBirthDate.value = date
        calculateAge()
    }

    fun updateTargetDate(date: LocalDate) {
        _ageTargetDate.value = date
        calculateAge()
    }

    fun calculateAge() {
        val birth = _ageBirthDate.value
        val target = _ageTargetDate.value
        if (birth.isAfter(target)) {
            _ageResult.value = null
            return
        }

        val period = Period.between(birth, target)
        val totalDays = ChronoUnit.DAYS.between(birth, target)
        val totalWeeks = totalDays / 7
        val totalMonths = ChronoUnit.MONTHS.between(birth, target)

        // Next Birthday Countdown
        val nextBdayThisYear = birth.withYear(target.year)
        val nextBday = if (nextBdayThisYear.isBefore(target) || nextBdayThisYear.isEqual(target)) {
            birth.withYear(target.year + 1)
        } else {
            nextBdayThisYear
        }

        val daysUntilNextBday = ChronoUnit.DAYS.between(target, nextBday)
        val nextBdayPeriod = Period.between(target, nextBday)
        val dayOfWeek = nextBday.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

        val result = AgeResult(
            years = period.years,
            months = period.months,
            days = period.days,
            totalMonths = totalMonths,
            totalWeeks = totalWeeks,
            totalDays = totalDays,
            daysUntilNextBirthday = daysUntilNextBday,
            nextBirthdayMonths = nextBdayPeriod.months,
            nextBirthdayDays = nextBdayPeriod.days,
            nextBirthdayDayOfWeek = dayOfWeek
        )
        _ageResult.value = result

        // Save to History
        val expr = "DOB: $birth | Target: $target"
        val res = "${period.years} Years, ${period.months} Months, ${period.days} Days"
        saveToHistory("Age", expr, res)
    }

    // --- 3. BMI Calculator State & Actions ---
    private val _bmiMode = MutableStateFlow("Metric") // Metric or Imperial
    val bmiMode: StateFlow<String> = _bmiMode.asStateFlow()

    private val _bmiWeightMetric = MutableStateFlow("70")
    val bmiWeightMetric: StateFlow<String> = _bmiWeightMetric.asStateFlow()

    private val _bmiHeightMetric = MutableStateFlow("175")
    val bmiHeightMetric: StateFlow<String> = _bmiHeightMetric.asStateFlow()

    private val _bmiWeightImperial = MutableStateFlow("150")
    val bmiWeightImperial: StateFlow<String> = _bmiWeightImperial.asStateFlow()

    private val _bmiHeightFeet = MutableStateFlow("5")
    val bmiHeightFeet: StateFlow<String> = _bmiHeightFeet.asStateFlow()

    private val _bmiHeightInches = MutableStateFlow("9")
    val bmiHeightInches: StateFlow<String> = _bmiHeightInches.asStateFlow()

    data class BmiResult(
        val bmi: Double,
        val status: String,
        val minHealthyWeight: Double,
        val maxHealthyWeight: Double,
        val unit: String
    )

    private val _bmiResult = MutableStateFlow<BmiResult?>(null)
    val bmiResult: StateFlow<BmiResult?> = _bmiResult.asStateFlow()

    fun updateBmiMode(mode: String) {
        _bmiMode.value = mode
        _bmiResult.value = null
    }

    fun onBmiValueChange(field: String, value: String) {
        val cleanValue = value.filter { it.isDigit() || it == '.' }
        when (field) {
            "weight_metric" -> _bmiWeightMetric.value = cleanValue
            "height_metric" -> _bmiHeightMetric.value = cleanValue
            "weight_imperial" -> _bmiWeightImperial.value = cleanValue
            "height_feet" -> _bmiHeightFeet.value = cleanValue
            "height_inches" -> _bmiHeightInches.value = cleanValue
        }
    }

    fun calculateBmi() {
        val mode = _bmiMode.value
        if (mode == "Metric") {
            val weight = _bmiWeightMetric.value.toDoubleOrNull() ?: return
            val height = _bmiHeightMetric.value.toDoubleOrNull() ?: return
            if (height <= 0 || weight <= 0) return

            val heightMeters = height / 100.0
            val bmiVal = weight / (heightMeters * heightMeters)
            val status = getBmiStatus(bmiVal)

            val minWeight = 18.5 * (heightMeters * heightMeters)
            val maxWeight = 24.9 * (heightMeters * heightMeters)

            _bmiResult.value = BmiResult(
                bmi = bmiVal,
                status = status,
                minHealthyWeight = minWeight,
                maxHealthyWeight = maxWeight,
                unit = "kg"
            )

            saveToHistory("BMI", "H: ${height}cm, W: ${weight}kg", "BMI: ${String.format("%.1f", bmiVal)} ($status)")
        } else {
            val weight = _bmiWeightImperial.value.toDoubleOrNull() ?: return
            val feet = _bmiHeightFeet.value.toDoubleOrNull() ?: 0.0
            val inches = _bmiHeightInches.value.toDoubleOrNull() ?: 0.0
            val totalInches = (feet * 12) + inches
            if (totalInches <= 0 || weight <= 0) return

            val bmiVal = (weight / (totalInches * totalInches)) * 703
            val status = getBmiStatus(bmiVal)

            val minWeight = (18.5 * (totalInches * totalInches)) / 703
            val maxWeight = (24.9 * (totalInches * totalInches)) / 703

            _bmiResult.value = BmiResult(
                bmi = bmiVal,
                status = status,
                minHealthyWeight = minWeight,
                maxHealthyWeight = maxWeight,
                unit = "lbs"
            )

            saveToHistory("BMI", "H: ${feet.toInt()}ft ${inches.toInt()}in, W: ${weight}lbs", "BMI: ${String.format("%.1f", bmiVal)} ($status)")
        }
    }

    private fun getBmiStatus(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
    }

    // --- 4. Currency Converter State & Actions ---
    private val _currencyRates = MutableStateFlow<Map<String, Double>>(CurrencyService.DEFAULT_RATES)
    val currencyRates: StateFlow<Map<String, Double>> = _currencyRates.asStateFlow()

    private val _currencyBase = MutableStateFlow("USD")
    val currencyBase: StateFlow<String> = _currencyBase.asStateFlow()

    private val _currencyTarget = MutableStateFlow("INR")
    val currencyTarget: StateFlow<String> = _currencyTarget.asStateFlow()

    private val _currencyAmount = MutableStateFlow("100")
    val currencyAmount: StateFlow<String> = _currencyAmount.asStateFlow()

    private val _currencyResult = MutableStateFlow("")
    val currencyResult: StateFlow<String> = _currencyResult.asStateFlow()

    private val _currencyLoading = MutableStateFlow(false)
    val currencyLoading: StateFlow<Boolean> = _currencyLoading.asStateFlow()

    init {
        syncCurrencyRates()
    }

    fun syncCurrencyRates() {
        viewModelScope.launch {
            _currencyLoading.value = true
            val fetched = CurrencyService.fetchLatestRates()
            _currencyRates.value = fetched
            _currencyLoading.value = false
            convertCurrency()
        }
    }

    fun updateCurrencyBase(base: String) {
        _currencyBase.value = base
        convertCurrency()
    }

    fun updateCurrencyTarget(target: String) {
        _currencyTarget.value = target
        convertCurrency()
    }

    fun updateCurrencyAmount(amount: String) {
        _currencyAmount.value = amount.filter { it.isDigit() || it == '.' }
        convertCurrency()
    }

    fun convertCurrency() {
        val amount = _currencyAmount.value.toDoubleOrNull() ?: 0.0
        val base = _currencyBase.value
        val target = _currencyTarget.value
        val rates = _currencyRates.value

        val baseRate = rates[base] ?: 1.0
        val targetRate = rates[target] ?: 1.0

        // Convert base to USD first, then to target
        val amountInUsd = amount / baseRate
        val converted = amountInUsd * targetRate

        val formattedResult = if (converted == 0.0) "0.00" else String.format("%.2f", converted)
        _currencyResult.value = formattedResult
    }

    fun saveCurrencyToHistory() {
        val amount = _currencyAmount.value
        val base = _currencyBase.value
        val target = _currencyTarget.value
        val res = _currencyResult.value
        if (amount.isNotEmpty() && res.isNotEmpty()) {
            saveToHistory("Currency", "$amount $base to $target", "$res $target")
        }
    }

    // --- 5. EMI Calculator State & Actions ---
    private val _emiPrincipal = MutableStateFlow("100000")
    val emiPrincipal: StateFlow<String> = _emiPrincipal.asStateFlow()

    private val _emiRate = MutableStateFlow("8.5")
    val emiRate: StateFlow<String> = _emiRate.asStateFlow()

    private val _emiTenure = MutableStateFlow("5")
    val emiTenure: StateFlow<String> = _emiTenure.asStateFlow()

    private val _emiTenureType = MutableStateFlow("Years") // Years or Months
    val emiTenureType: StateFlow<String> = _emiTenureType.asStateFlow()

    data class EmiResultData(
        val monthlyEmi: Double,
        val totalInterest: Double,
        val totalPayment: Double
    )

    private val _emiResult = MutableStateFlow<EmiResultData?>(null)
    val emiResult: StateFlow<EmiResultData?> = _emiResult.asStateFlow()

    fun onEmiValueChange(field: String, value: String) {
        val cleanValue = value.filter { it.isDigit() || it == '.' }
        when (field) {
            "principal" -> _emiPrincipal.value = cleanValue
            "rate" -> _emiRate.value = cleanValue
            "tenure" -> _emiTenure.value = cleanValue
        }
    }

    fun updateEmiTenureType(type: String) {
        _emiTenureType.value = type
    }

    fun calculateEmi() {
        val p = _emiPrincipal.value.toDoubleOrNull() ?: return
        val annualRate = _emiRate.value.toDoubleOrNull() ?: return
        val tenureVal = _emiTenure.value.toDoubleOrNull() ?: return
        if (p <= 0 || annualRate < 0 || tenureVal <= 0) return

        val months = if (_emiTenureType.value == "Years") tenureVal * 12 else tenureVal
        val monthlyRate = annualRate / (12 * 100)

        val emi = if (monthlyRate == 0.0) {
            p / months
        } else {
            (p * monthlyRate * (1 + monthlyRate).pow(months)) / ((1 + monthlyRate).pow(months) - 1)
        }

        val totalPayment = emi * months
        val totalInterest = totalPayment - p

        _emiResult.value = EmiResultData(
            monthlyEmi = emi,
            totalInterest = totalInterest,
            totalPayment = totalPayment
        )

        val expr = "Principal: $p, Rate: $annualRate%, Tenure: ${tenureVal.toInt()} ${_emiTenureType.value}"
        val res = "EMI: ${String.format("%.2f", emi)}"
        saveToHistory("EMI", expr, res)
    }

    // --- 6. GST Calculator State & Actions ---
    private val _gstAmount = MutableStateFlow("1000")
    val gstAmount: StateFlow<String> = _gstAmount.asStateFlow()

    private val _gstRate = MutableStateFlow("18") // Percent
    val gstRate: StateFlow<String> = _gstRate.asStateFlow()

    private val _gstType = MutableStateFlow("Exclusive") // Exclusive (Add) or Inclusive (Subtract)
    val gstType: StateFlow<String> = _gstType.asStateFlow()

    data class GstResultData(
        val netAmount: Double,
        val gstAmount: Double,
        val totalAmount: Double
    )

    private val _gstResult = MutableStateFlow<GstResultData?>(null)
    val gstResult: StateFlow<GstResultData?> = _gstResult.asStateFlow()

    fun onGstValueChange(field: String, value: String) {
        val clean = value.filter { it.isDigit() || it == '.' }
        if (field == "amount") _gstAmount.value = clean
        if (field == "rate") _gstRate.value = clean
    }

    fun updateGstType(type: String) {
        _gstType.value = type
        calculateGst()
    }

    fun calculateGst() {
        val amount = _gstAmount.value.toDoubleOrNull() ?: return
        val rate = _gstRate.value.toDoubleOrNull() ?: 0.0
        if (amount <= 0 || rate < 0) return

        val result = if (_gstType.value == "Exclusive") {
            val gst = amount * (rate / 100.0)
            val total = amount + gst
            GstResultData(netAmount = amount, gstAmount = gst, totalAmount = total)
        } else {
            val net = amount / (1 + (rate / 100.0))
            val gst = amount - net
            GstResultData(netAmount = net, gstAmount = gst, totalAmount = amount)
        }
        _gstResult.value = result

        val expr = "Base: $amount, GST: $rate% (${_gstType.value})"
        val res = "Total: ${String.format("%.2f", result.totalAmount)}"
        saveToHistory("GST", expr, res)
    }

    // --- 7. Percentage / Discount / Profit & Loss State & Actions ---
    private val _percentageTab = MutableStateFlow("Percentage") // Percentage, Discount, Profit-Loss
    val percentageTab: StateFlow<String> = _percentageTab.asStateFlow()

    fun updatePercentageTab(tab: String) {
        _percentageTab.value = tab
    }

    // Tab 7.1 Percentage Sub-modes
    private val _pctMode = MutableStateFlow("Mode1") // Mode1: X% of Y, Mode2: X is what % of Y, Mode3: Increase/Decrease from X to Y
    val pctMode: StateFlow<String> = _pctMode.asStateFlow()

    private val _pctVal1 = MutableStateFlow("15")
    val pctVal1: StateFlow<String> = _pctVal1.asStateFlow()

    private val _pctVal2 = MutableStateFlow("200")
    val pctVal2: StateFlow<String> = _pctVal2.asStateFlow()

    private val _pctResult = MutableStateFlow("")
    val pctResult: StateFlow<String> = _pctResult.asStateFlow()

    fun updatePctMode(mode: String) {
        _pctMode.value = mode
        _pctResult.value = ""
    }

    fun onPctValueChange(field: String, value: String) {
        val clean = value.filter { it.isDigit() || it == '.' || it == '-' }
        if (field == "val1") _pctVal1.value = clean
        if (field == "val2") _pctVal2.value = clean
    }

    fun calculatePercentage() {
        val x = _pctVal1.value.toDoubleOrNull() ?: return
        val y = _pctVal2.value.toDoubleOrNull() ?: return

        when (_pctMode.value) {
            "Mode1" -> {
                val res = (x / 100.0) * y
                val formatted = formatResult(res)
                _pctResult.value = formatted
                saveToHistory("Percentage", "$x% of $y", formatted)
            }
            "Mode2" -> {
                if (y == 0.0) {
                    _pctResult.value = "Division by zero"
                    return
                }
                val res = (x / y) * 100.0
                val formatted = formatResult(res) + "%"
                _pctResult.value = formatted
                saveToHistory("Percentage", "$x is what % of $y", formatted)
            }
            "Mode3" -> {
                if (x == 0.0) {
                    _pctResult.value = "Invalid initial value"
                    return
                }
                val change = y - x
                val pctChange = (change / x) * 100.0
                val formatted = (if (pctChange >= 0) "+" else "") + formatResult(pctChange) + "%"
                _pctResult.value = formatted
                saveToHistory("Percentage", "Change from $x to $y", formatted)
            }
        }
    }

    // Tab 7.2 Discount Calculator
    private val _discPrice = MutableStateFlow("1200")
    val discPrice: StateFlow<String> = _discPrice.asStateFlow()

    private val _discPercent = MutableStateFlow("20")
    val discPercent: StateFlow<String> = _discPercent.asStateFlow()

    private val _discTax = MutableStateFlow("5")
    val discTax: StateFlow<String> = _discTax.asStateFlow()

    data class DiscountResultData(
        val finalPrice: Double,
        val discountAmount: Double,
        val taxAmount: Double,
        val totalSavings: Double
    )

    private val _discResult = MutableStateFlow<DiscountResultData?>(null)
    val discResult: StateFlow<DiscountResultData?> = _discResult.asStateFlow()

    fun onDiscValueChange(field: String, value: String) {
        val clean = value.filter { it.isDigit() || it == '.' }
        when (field) {
            "price" -> _discPrice.value = clean
            "percent" -> _discPercent.value = clean
            "tax" -> _discTax.value = clean
        }
    }

    fun calculateDiscount() {
        val price = _discPrice.value.toDoubleOrNull() ?: return
        val disc = _discPercent.value.toDoubleOrNull() ?: 0.0
        val tax = _discTax.value.toDoubleOrNull() ?: 0.0
        if (price <= 0 || disc < 0 || tax < 0) return

        val discAmt = price * (disc / 100.0)
        val discountedPrice = price - discAmt
        val taxAmt = discountedPrice * (tax / 100.0)
        val finalPrice = discountedPrice + taxAmt
        val savings = price - finalPrice + taxAmt // Savings is original price - final price + tax? Or simple price - finalPrice? Let's say pure savings is original - discounted price (i.e. discAmt)
        
        _discResult.value = DiscountResultData(
            finalPrice = finalPrice,
            discountAmount = discAmt,
            taxAmount = taxAmt,
            totalSavings = discAmt
        )

        val expr = "Price: $price, Disc: $disc%, Tax: $tax%"
        val res = "Final: ${String.format("%.2f", finalPrice)}"
        saveToHistory("Percentage", expr, res)
    }

    // Tab 7.3 Profit & Loss
    private val _plCostPrice = MutableStateFlow("500")
    val plCostPrice: StateFlow<String> = _plCostPrice.asStateFlow()

    private val _plSellingPrice = MutableStateFlow("650")
    val plSellingPrice: StateFlow<String> = _plSellingPrice.asStateFlow()

    data class ProfitLossResultData(
        val difference: Double,
        val pctDifference: Double,
        val status: String
    )

    private val _plResult = MutableStateFlow<ProfitLossResultData?>(null)
    val plResult: StateFlow<ProfitLossResultData?> = _plResult.asStateFlow()

    fun onPlValueChange(field: String, value: String) {
        val clean = value.filter { it.isDigit() || it == '.' }
        if (field == "cost") _plCostPrice.value = clean
        if (field == "selling") _plSellingPrice.value = clean
    }

    fun calculateProfitLoss() {
        val cp = _plCostPrice.value.toDoubleOrNull() ?: return
        val sp = _plSellingPrice.value.toDoubleOrNull() ?: return
        if (cp <= 0 || sp < 0) return

        val diff = sp - cp
        val pct = (diff / cp) * 100.0
        val status = when {
            diff > 0 -> "Profit"
            diff < 0 -> "Loss"
            else -> "No Profit/Loss"
        }

        _plResult.value = ProfitLossResultData(
            difference = diff,
            pctDifference = pct,
            status = status
        )

        val expr = "Cost: $cp, Selling: $sp"
        val res = "$status: ${String.format("%.2f", diff)} (${String.format("%.1f", pct)}%)"
        saveToHistory("Percentage", expr, res)
    }
}

class CalculatorViewModelFactory(private val repository: CalculationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
