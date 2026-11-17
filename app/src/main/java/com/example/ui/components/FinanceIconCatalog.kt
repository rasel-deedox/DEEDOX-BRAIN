package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CarRepair
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CreditScore
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EventSeat
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

enum class FinanceIconCategory(
    val id: String,
    val pillLabel: String,
    val fullTitle: String
) {
    FOOD("food", "Food", "Food & Dining"),
    TRANSPORT("transport", "Transport", "Transport & Travel"),
    BILLS("bills", "Bills", "Bills & Utilities"),
    BUSINESS("business", "Business", "Business, Tech & Work"),
    SHOPPING("shopping", "Shopping", "Shopping & Lifestyle"),
    HEALTH("health", "Health", "Health & Fitness"),
    EDUCATION("education", "Education", "Education & Growth"),
    FINANCE("finance", "Income", "Finance, Income & Savings")
}

@Immutable
data class FinanceIconSpec(
    val key: String,
    val label: String,
    val category: FinanceIconCategory,
    val vector: ImageVector,
    val keywords: List<String>
)

object FinanceIconCatalog {

    val allIcons: List<FinanceIconSpec> = listOf(
        // 1. Food & Dining (8)
        FinanceIconSpec(
            key = "coffee",
            label = "Coffee / Tea",
            category = FinanceIconCategory.FOOD,
            vector = Icons.Outlined.LocalCafe,
            keywords = listOf("coffee", "tea", "cafe", "latte", "espresso", "matcha", "boba")
        ),
        FinanceIconSpec(
            key = "restaurant",
            label = "Restaurant",
            category = FinanceIconCategory.FOOD,
            vector = Icons.Outlined.Restaurant,
            keywords = listOf("restaurant", "meal", "lunch", "dinner", "dining", "food", "eat")
        ),
        FinanceIconSpec(
            key = "groceries",
            label = "Groceries",
            category = FinanceIconCategory.FOOD,
            vector = Icons.Outlined.ShoppingCart,
            keywords = listOf("groceries", "supermarket", "grocery", "market", "produce", "cart")
        ),
        FinanceIconSpec(
            key = "fast_food",
            label = "Fast Food",
            category = FinanceIconCategory.FOOD,
            vector = Icons.Outlined.Fastfood,
            keywords = listOf("fast food", "snacks", "burger", "pizza", "fries", "snack")
        ),
        FinanceIconSpec(
            key = "beverages",
            label = "Beverages",
            category = FinanceIconCategory.FOOD,
            vector = Icons.Outlined.LocalBar,
            keywords = listOf("water", "beverages", "drink", "juice", "soda", "smoothie")
        ),
        FinanceIconSpec(
            key = "bakery",
            label = "Bakery / Sweets",
            category = FinanceIconCategory.FOOD,
            vector = Icons.Outlined.BakeryDining,
            keywords = listOf("bakery", "sweets", "bread", "cake", "pastry", "croissant", "dessert")
        ),
        FinanceIconSpec(
            key = "dessert",
            label = "Treats",
            category = FinanceIconCategory.FOOD,
            vector = Icons.Outlined.Icecream,
            keywords = listOf("ice cream", "treat", "dessert", "gelato", "sweet")
        ),
        FinanceIconSpec(
            key = "food_delivery",
            label = "Delivery",
            category = FinanceIconCategory.FOOD,
            vector = Icons.Outlined.DeliveryDining,
            keywords = listOf("delivery", "takeout", "courier", "uber eats", "doordash")
        ),

        // 2. Transport & Travel (8)
        FinanceIconSpec(
            key = "fuel",
            label = "Fuel / Gas",
            category = FinanceIconCategory.TRANSPORT,
            vector = Icons.Outlined.LocalGasStation,
            keywords = listOf("fuel", "gas", "petrol", "diesel", "ev", "charging", "station")
        ),
        FinanceIconSpec(
            key = "taxi",
            label = "Ride-Share",
            category = FinanceIconCategory.TRANSPORT,
            vector = Icons.Outlined.LocalTaxi,
            keywords = listOf("taxi", "ride", "rideshare", "uber", "lyft", "cab")
        ),
        FinanceIconSpec(
            key = "transit",
            label = "Bus / Metro",
            category = FinanceIconCategory.TRANSPORT,
            vector = Icons.Outlined.DirectionsBus,
            keywords = listOf("bus", "train", "metro", "subway", "transit", "commute", "rail")
        ),
        FinanceIconSpec(
            key = "flight",
            label = "Flight",
            category = FinanceIconCategory.TRANSPORT,
            vector = Icons.Outlined.Flight,
            keywords = listOf("flight", "tickets", "airline", "plane", "airport", "travel", "trip")
        ),
        FinanceIconSpec(
            key = "car_service",
            label = "Maintenance",
            category = FinanceIconCategory.TRANSPORT,
            vector = Icons.Outlined.CarRepair,
            keywords = listOf("vehicle", "maintenance", "car", "repair", "mechanic", "auto", "service")
        ),
        FinanceIconSpec(
            key = "parking",
            label = "Parking / Toll",
            category = FinanceIconCategory.TRANSPORT,
            vector = Icons.Outlined.LocalParking,
            keywords = listOf("parking", "toll", "garage", "valet", "highway")
        ),
        FinanceIconSpec(
            key = "car",
            label = "Car / Drive",
            category = FinanceIconCategory.TRANSPORT,
            vector = Icons.Outlined.DirectionsCar,
            keywords = listOf("car", "drive", "vehicle", "auto", "lease", "commute")
        ),
        FinanceIconSpec(
            key = "hotel",
            label = "Hotel / Stay",
            category = FinanceIconCategory.TRANSPORT,
            vector = Icons.Outlined.Hotel,
            keywords = listOf("hotel", "stay", "lodging", "airbnb", "resort", "vacation")
        ),

        // 3. Bills & Utilities (8)
        FinanceIconSpec(
            key = "electricity",
            label = "Electricity",
            category = FinanceIconCategory.BILLS,
            vector = Icons.Outlined.Bolt,
            keywords = listOf("electricity", "power", "energy", "electric", "utility", "bill")
        ),
        FinanceIconSpec(
            key = "wifi",
            label = "Internet / Wi-Fi",
            category = FinanceIconCategory.BILLS,
            vector = Icons.Outlined.Wifi,
            keywords = listOf("internet", "wifi", "wi-fi", "broadband", "fiber", "network")
        ),
        FinanceIconSpec(
            key = "mobile_data",
            label = "Mobile / Data",
            category = FinanceIconCategory.BILLS,
            vector = Icons.Outlined.Smartphone,
            keywords = listOf("mobile", "recharge", "data", "phone", "sim", "cellular", "plan")
        ),
        FinanceIconSpec(
            key = "rent",
            label = "House Rent",
            category = FinanceIconCategory.BILLS,
            vector = Icons.Outlined.Home,
            keywords = listOf("house", "rent", "apartment", "lease", "mortgage", "housing")
        ),
        FinanceIconSpec(
            key = "water_utility",
            label = "Water / Gas",
            category = FinanceIconCategory.BILLS,
            vector = Icons.Outlined.WaterDrop,
            keywords = listOf("water", "gas", "utility", "bill", "plumbing")
        ),
        FinanceIconSpec(
            key = "streaming",
            label = "Streaming / TV",
            category = FinanceIconCategory.BILLS,
            vector = Icons.Outlined.LiveTv,
            keywords = listOf("tv", "streaming", "subscription", "netflix", "spotify", "cable")
        ),
        FinanceIconSpec(
            key = "heating",
            label = "Heating / AC",
            category = FinanceIconCategory.BILLS,
            vector = Icons.Outlined.Thermostat,
            keywords = listOf("heating", "cooling", "ac", "thermostat", "climate")
        ),
        FinanceIconSpec(
            key = "insurance",
            label = "Insurance",
            category = FinanceIconCategory.BILLS,
            vector = Icons.Outlined.Security,
            keywords = listOf("insurance", "policy", "protection", "coverage", "security")
        ),

        // 4. Business, Tech & Work (8)
        FinanceIconSpec(
            key = "saas",
            label = "Software / SaaS",
            category = FinanceIconCategory.BUSINESS,
            vector = Icons.Outlined.Terminal,
            keywords = listOf("software", "saas", "subscription", "license", "app", "dev", "tool")
        ),
        FinanceIconSpec(
            key = "cloud_api",
            label = "Cloud / API",
            category = FinanceIconCategory.BUSINESS,
            vector = Icons.Outlined.Cloud,
            keywords = listOf("cloud", "hosting", "api", "aws", "gcp", "firebase", "vercel")
        ),
        FinanceIconSpec(
            key = "hardware",
            label = "Hardware",
            category = FinanceIconCategory.BUSINESS,
            vector = Icons.Outlined.LaptopMac,
            keywords = listOf("hardware", "gadgets", "laptop", "computer", "mac", "monitor", "gear")
        ),
        FinanceIconSpec(
            key = "office",
            label = "Office Supplies",
            category = FinanceIconCategory.BUSINESS,
            vector = Icons.Outlined.WorkOutline,
            keywords = listOf("office", "supplies", "work", "briefcase", "desk", "coworking")
        ),
        FinanceIconSpec(
            key = "marketing",
            label = "Marketing / Ads",
            category = FinanceIconCategory.BUSINESS,
            vector = Icons.Outlined.Campaign,
            keywords = listOf("marketing", "ads", "campaign", "promotion", "sponsor", "growth")
        ),
        FinanceIconSpec(
            key = "freelance",
            label = "Client Payout",
            category = FinanceIconCategory.BUSINESS,
            vector = Icons.Outlined.Handshake,
            keywords = listOf("freelance", "client", "payout", "contract", "retainer", "agency")
        ),
        FinanceIconSpec(
            key = "server",
            label = "Servers / DB",
            category = FinanceIconCategory.BUSINESS,
            vector = Icons.Outlined.Dns,
            keywords = listOf("server", "database", "infra", "backend", "vps", "rack")
        ),
        FinanceIconSpec(
            key = "domain",
            label = "Domains / Web",
            category = FinanceIconCategory.BUSINESS,
            vector = Icons.Outlined.Language,
            keywords = listOf("domain", "web", "website", "dns", "ssl")
        ),

        // 5. Shopping & Lifestyle (8)
        FinanceIconSpec(
            key = "clothing",
            label = "Clothing",
            category = FinanceIconCategory.SHOPPING,
            vector = Icons.Outlined.Checkroom,
            keywords = listOf("clothing", "apparel", "shirt", "fashion", "wardrobe", "outfit")
        ),
        FinanceIconSpec(
            key = "footwear",
            label = "Footwear",
            category = FinanceIconCategory.SHOPPING,
            vector = Icons.Outlined.Hiking,
            keywords = listOf("footwear", "shoes", "sneakers", "boots", "sandals")
        ),
        FinanceIconSpec(
            key = "electronics",
            label = "Electronics",
            category = FinanceIconCategory.SHOPPING,
            vector = Icons.Outlined.Headphones,
            keywords = listOf("electronics", "headphones", "audio", "camera", "speaker", "tech")
        ),
        FinanceIconSpec(
            key = "home_decor",
            label = "Home Decor",
            category = FinanceIconCategory.SHOPPING,
            vector = Icons.Outlined.Weekend,
            keywords = listOf("home", "decor", "furniture", "sofa", "chair", "interior")
        ),
        FinanceIconSpec(
            key = "gifts",
            label = "Gifts",
            category = FinanceIconCategory.SHOPPING,
            vector = Icons.Outlined.CardGiftcard,
            keywords = listOf("gifts", "gift", "present", "birthday", "anniversary", "party")
        ),
        FinanceIconSpec(
            key = "personal_care",
            label = "Personal Care",
            category = FinanceIconCategory.SHOPPING,
            vector = Icons.Outlined.Spa,
            keywords = listOf("cosmetics", "personal care", "skincare", "beauty", "salon", "grooming")
        ),
        FinanceIconSpec(
            key = "shopping_bag",
            label = "Retail",
            category = FinanceIconCategory.SHOPPING,
            vector = Icons.Outlined.ShoppingBag,
            keywords = listOf("shopping", "retail", "store", "mall", "bag", "purchase")
        ),
        FinanceIconSpec(
            key = "pets",
            label = "Pet Care",
            category = FinanceIconCategory.SHOPPING,
            vector = Icons.Outlined.Pets,
            keywords = listOf("pet", "dog", "cat", "vet", "pet food", "lifestyle")
        ),

        // 6. Health & Fitness (7)
        FinanceIconSpec(
            key = "pharmacy",
            label = "Pharmacy",
            category = FinanceIconCategory.HEALTH,
            vector = Icons.Outlined.Medication,
            keywords = listOf("pharmacy", "medicine", "pills", "prescription", "drugstore")
        ),
        FinanceIconSpec(
            key = "hospital",
            label = "Doctor",
            category = FinanceIconCategory.HEALTH,
            vector = Icons.Outlined.LocalHospital,
            keywords = listOf("doctor", "hospital", "clinic", "medical", "checkup", "health")
        ),
        FinanceIconSpec(
            key = "gym",
            label = "Gym / Workout",
            category = FinanceIconCategory.HEALTH,
            vector = Icons.Outlined.FitnessCenter,
            keywords = listOf("gym", "workout", "fitness", "membership", "weights", "training")
        ),
        FinanceIconSpec(
            key = "supplements",
            label = "Supplements",
            category = FinanceIconCategory.HEALTH,
            vector = Icons.Outlined.HealthAndSafety,
            keywords = listOf("supplements", "vitamins", "protein", "nutrition", "health")
        ),
        FinanceIconSpec(
            key = "dental_optical",
            label = "Dental / Optical",
            category = FinanceIconCategory.HEALTH,
            vector = Icons.Outlined.Visibility,
            keywords = listOf("dental", "optical", "glasses", "eye", "dentist", "vision")
        ),
        FinanceIconSpec(
            key = "wellness",
            label = "Wellness",
            category = FinanceIconCategory.HEALTH,
            vector = Icons.Outlined.SelfImprovement,
            keywords = listOf("wellness", "yoga", "meditation", "mindfulness", "therapy")
        ),
        FinanceIconSpec(
            key = "cardio",
            label = "Sports / Run",
            category = FinanceIconCategory.HEALTH,
            vector = Icons.Outlined.DirectionsRun,
            keywords = listOf("sport", "running", "cardio", "marathon", "gear")
        ),

        // 7. Education & Growth (7)
        FinanceIconSpec(
            key = "books",
            label = "Books / Courses",
            category = FinanceIconCategory.EDUCATION,
            vector = Icons.Outlined.MenuBook,
            keywords = listOf("books", "courses", "reading", "ebook", "kindle", "learning")
        ),
        FinanceIconSpec(
            key = "tuition",
            label = "Tuition / Fees",
            category = FinanceIconCategory.EDUCATION,
            vector = Icons.Outlined.School,
            keywords = listOf("tuition", "fees", "university", "college", "school", "degree")
        ),
        FinanceIconSpec(
            key = "workshops",
            label = "Workshops",
            category = FinanceIconCategory.EDUCATION,
            vector = Icons.Outlined.EventSeat,
            keywords = listOf("workshops", "events", "conference", "seminar", "summit", "ticket")
        ),
        FinanceIconSpec(
            key = "ai_research",
            label = "AI Tools",
            category = FinanceIconCategory.EDUCATION,
            vector = Icons.Outlined.Psychology,
            keywords = listOf("ai", "tools", "research", "llm", "chatgpt", "claude", "gemini")
        ),
        FinanceIconSpec(
            key = "certificate",
            label = "Certification",
            category = FinanceIconCategory.EDUCATION,
            vector = Icons.Outlined.WorkspacePremium,
            keywords = listOf("certificate", "exam", "award", "skill", "license")
        ),
        FinanceIconSpec(
            key = "stationery",
            label = "Notes / Study",
            category = FinanceIconCategory.EDUCATION,
            vector = Icons.Outlined.EditNote,
            keywords = listOf("stationery", "notebook", "study", "writing", "notes")
        ),
        FinanceIconSpec(
            key = "coding",
            label = "Bootcamp",
            category = FinanceIconCategory.EDUCATION,
            vector = Icons.Outlined.Code,
            keywords = listOf("coding", "bootcamp", "programming", "developer", "course")
        ),

        // 8. Finance, Income & Savings (10)
        FinanceIconSpec(
            key = "salary",
            label = "Salary / Payroll",
            category = FinanceIconCategory.FINANCE,
            vector = Icons.Outlined.Payments,
            keywords = listOf("salary", "payroll", "paycheck", "wages", "income", "stipend")
        ),
        FinanceIconSpec(
            key = "profit",
            label = "Business Profit",
            category = FinanceIconCategory.FINANCE,
            vector = Icons.Outlined.TrendingUp,
            keywords = listOf("profit", "revenue", "business", "earnings", "margin", "sales")
        ),
        FinanceIconSpec(
            key = "stocks",
            label = "Investments",
            category = FinanceIconCategory.FINANCE,
            vector = Icons.Outlined.ShowChart,
            keywords = listOf("investments", "stocks", "crypto", "etf", "portfolio", "shares")
        ),
        FinanceIconSpec(
            key = "savings",
            label = "Savings Vault",
            category = FinanceIconCategory.FINANCE,
            vector = Icons.Outlined.Savings,
            keywords = listOf("savings", "vault", "reserve", "emergency fund", "piggy bank")
        ),
        FinanceIconSpec(
            key = "loan_emi",
            label = "Loan / EMI",
            category = FinanceIconCategory.FINANCE,
            vector = Icons.Outlined.CreditScore,
            keywords = listOf("loan", "emi", "credit", "installment", "debt", "repayment")
        ),
        FinanceIconSpec(
            key = "charity",
            label = "Charity",
            category = FinanceIconCategory.FINANCE,
            vector = Icons.Outlined.VolunteerActivism,
            keywords = listOf("charity", "donation", "giving", "zakat", "tithe", "nonprofit")
        ),
        FinanceIconSpec(
            key = "wallet",
            label = "Wallet / Cash",
            category = FinanceIconCategory.FINANCE,
            vector = Icons.Outlined.AccountBalanceWallet,
            keywords = listOf("wallet", "cash", "pocket", "general", "balance")
        ),
        FinanceIconSpec(
            key = "bank",
            label = "Bank / Wire",
            category = FinanceIconCategory.FINANCE,
            vector = Icons.Outlined.AccountBalance,
            keywords = listOf("bank", "wire", "transfer", "deposit", "treasury", "checking")
        ),
        FinanceIconSpec(
            key = "taxes",
            label = "Taxes / Audit",
            category = FinanceIconCategory.FINANCE,
            vector = Icons.Outlined.ReceiptLong,
            keywords = listOf("taxes", "tax", "invoice", "receipt", "vat", "accounting")
        ),
        FinanceIconSpec(
            key = "bonus",
            label = "Bonus / Cashflow",
            category = FinanceIconCategory.FINANCE,
            vector = Icons.Outlined.AttachMoney,
            keywords = listOf("bonus", "dividend", "cashflow", "income", "dollar", "money")
        )
    )

    private val byKey: Map<String, FinanceIconSpec> = allIcons.associateBy { it.key }

    private val legacyAliases: Map<String, String> = mapOf(
        "briefcase" to "office",
        "sparkle" to "saas",
        "send" to "marketing",
        "heart" to "restaurant",
        "compass" to "car",
        "star" to "stocks",
        "check" to "salary",
        "wallet" to "wallet"
    )

    val defaultFallbackRecentKeys: List<String> = listOf(
        "coffee",
        "restaurant",
        "fuel",
        "rent",
        "salary"
    )

    fun normalizeKey(key: String?): String {
        val raw = key?.trim()?.lowercase().orEmpty()
        if (raw.isBlank()) return "wallet"
        if (byKey.containsKey(raw)) return raw
        return legacyAliases[raw] ?: "wallet"
    }

    fun resolve(key: String?): FinanceIconSpec {
        val normalized = normalizeKey(key)
        return byKey[normalized] ?: byKey.getValue("wallet")
    }

    fun search(
        query: String,
        category: FinanceIconCategory? = null
    ): List<FinanceIconSpec> {
        val q = query.trim().lowercase()
        return allIcons.filter { spec ->
            val categoryMatches = category == null || spec.category == category
            if (!categoryMatches) return@filter false
            if (q.isBlank()) return@filter true
            spec.label.lowercase().contains(q) ||
                spec.key.lowercase().contains(q) ||
                spec.category.fullTitle.lowercase().contains(q) ||
                spec.category.pillLabel.lowercase().contains(q) ||
                spec.keywords.any { it.contains(q) }
        }
    }

    fun resolveRecentSpecs(
        recentKeys: List<String>,
        fallbackSectorKeys: List<String> = emptyList(),
        limit: Int = 5
    ): List<FinanceIconSpec> {
        val orderedKeys = (recentKeys + fallbackSectorKeys + defaultFallbackRecentKeys)
            .map { normalizeKey(it) }
            .distinct()
            .take(limit)
        return orderedKeys.map { resolve(it) }
    }
}
