package com.example.actitracker.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

data class IconInfo(
    val name: String,
    val icon: ImageVector? = null,
    val assetPath: String? = null,
    val category: String,
    val tags: List<String>,
    val emoji: String = "●"
)

object IconMapper {

    fun getCategoryRes(category: String): Int? {
        return when (category) {
            "General" -> com.example.actitracker.R.string.category_general
            "Activities" -> com.example.actitracker.R.string.category_activities
            "Work & Study" -> com.example.actitracker.R.string.category_work_study
            "Nature" -> com.example.actitracker.R.string.category_nature
            "Food & Drinks" -> com.example.actitracker.R.string.category_food_drinks
            "Leisure & Travel" -> com.example.actitracker.R.string.category_leisure
            "Other Symbols" -> com.example.actitracker.R.string.category_symbols
            else -> null
        }
    }

    fun getEmoji(): String {
        return "●"
    }

    fun getIconInfo(name: String): IconInfo? {
        if (name.contains(":")) {
            val parts = name.split(":")
            if (parts.size == 2) {
                val set = parts[0]
                val id = parts[1]
                val category = mapToLogicalCategory(id)
                return IconInfo(
                    name = name,
                    assetPath = "file:///android_asset/icons/$set/$id.svg",
                    category = category,
                    tags = emptyList()
                )
            }
        }
        return null
    }

    fun mapToLogicalCategory(id: String): String {
        val lowerId = id.lowercase()
        return when {
            // LEISURE & TRAVEL: transport, leisure, rest
            lowerId.contains("travel") || lowerId.contains("plane")
                    || lowerId.contains("airplane")
                    || lowerId.contains("airport") || lowerId.contains("hotel")
                    || lowerId.contains("bed") || lowerId.contains("sleep")
                    || lowerId.contains("beach") || lowerId.contains("palm")
                    || lowerId.contains("suitcase") || lowerId.contains("baggage")
                    || lowerId.contains("luggage") || lowerId.contains("camera")
                    || lowerId.contains("photo") || lowerId.contains("map")
                    || lowerId.contains("compass") || lowerId.contains("ticket")
                    || lowerId.contains("passport") || lowerId.contains("vacation")
                    || lowerId.contains("holiday") || lowerId.contains("resort")
                    || lowerId.contains("tent") || lowerId.contains("camp")
                    || lowerId.contains("caravan") || lowerId.contains("bus")
                    || lowerId.contains("train") || lowerId.contains("yacht")
                    || lowerId.contains("anchor") || lowerId.contains("boat")
                    || lowerId.contains("game") || lowerId.contains("controller")
                    || lowerId.contains("puzzle") || lowerId.contains("relax")
                    || lowerId.contains("spa") || lowerId.contains("theater")
                    || lowerId.contains("movie") || lowerId.contains("cinema")
                    || lowerId.contains("concert") || lowerId.contains("music")
                        -> "Leisure & Travel"

            // ACTIVITIES: expanded keywords (removed yacht, boat, music)
            lowerId.contains("bike") || lowerId.contains("run")
                    || lowerId.contains("walk") || lowerId.contains("sport")
                    || lowerId.contains("gym") || lowerId.contains("fitness")
                    || lowerId.contains("exercise") || lowerId.contains("ball")
                    || lowerId.contains("swim") || lowerId.contains("yoga")
                    || lowerId.contains("dance") || lowerId.contains("hike")
                    || lowerId.contains("climb") || lowerId.contains("tennis")
                    || lowerId.contains("soccer") || lowerId.contains("football")
                    || lowerId.contains("basketball") || lowerId.contains("medal")
                    || lowerId.contains("trophy") || lowerId.contains("dumbbell")
                    || lowerId.contains("pulse") || lowerId.contains("skip")
                    || lowerId.contains("jump") || lowerId.contains("skate")
                    || lowerId.contains("ski") || lowerId.contains("surf")
                    || lowerId.contains("paddle") || lowerId.contains("row")
                    || lowerId.contains("athlete") || lowerId.contains("stretching")
                        -> "Activities"

            // FOOD & DRINKS
            lowerId.contains("food") || lowerId.contains("eat")
                    || lowerId.contains("drink") || lowerId.contains("coffee")
                    || lowerId.contains("apple") || lowerId.contains("pizza")
                    || lowerId.contains("cake") || lowerId.contains("restaurant")
                    || lowerId.contains("bread") || lowerId.contains("milk")
                    || lowerId.contains("wine") || lowerId.contains("beer")
                    || lowerId.contains("chef") || lowerId.contains("cook")
                    || lowerId.contains("kitchen") || lowerId.contains("meat")
                    || lowerId.contains("egg") || lowerId.contains("fruit")
                    || lowerId.contains("vegetable") || lowerId.contains("bowl")
                    || lowerId.contains("cup") || lowerId.contains("glass")
                    || lowerId.contains("mug") || lowerId.contains("plate")
                    || lowerId.contains("fork") || lowerId.contains("knife")
                    || lowerId.contains("spoon") || lowerId.contains("breakfast")
                    || lowerId.contains("lunch") || lowerId.contains("dinner")
                    || lowerId.contains("snack") || lowerId.contains("bottle")
                    || lowerId.contains("carrot")
                        -> "Food & Drinks"

            // WORK & STUDY
            lowerId.contains("work") || lowerId.contains("study")
                    || lowerId.contains("school") || lowerId.contains("book")
                    || lowerId.contains("code") || lowerId.contains("chart")
                    || lowerId.contains("pencil") || lowerId.contains("computer")
                    || lowerId.contains("pen") || lowerId.contains("paper")
                    || lowerId.contains("notebook") || lowerId.contains("office")
                    || lowerId.contains("college") || lowerId.contains("laptop")
                    || lowerId.contains("desktop") || lowerId.contains("keyboard")
                    || lowerId.contains("mouse") || lowerId.contains("meeting")
                    || lowerId.contains("briefcase") || lowerId.contains("folder")
                    || lowerId.contains("report") || lowerId.contains("project")
                    || lowerId.contains("homework") || lowerId.contains("graduation")
                         -> "Work & Study"

            // NATURE
            lowerId.contains("tree") || lowerId.contains("nature")
                    || lowerId.contains("sun") || lowerId.contains("rain")
                    || lowerId.contains("cloud") || lowerId.contains("mountain")
                    || lowerId.contains("leaf") || lowerId.contains("flower")
                    || lowerId.contains("plant") || lowerId.contains("moon")
                    || lowerId.contains("star") || lowerId.contains("weather")
                    || lowerId.contains("sea") || lowerId.contains("ocean")
                    || lowerId.contains("wind") || lowerId.contains("storm")
                    || lowerId.contains("snow") || lowerId.contains("pet")
                    || lowerId.contains("dog") || lowerId.contains("cat")
                    || lowerId.contains("animal") || lowerId.contains("bird")
                    || lowerId.contains("fish") || lowerId.contains("bug")
                    || lowerId.contains("earth") || lowerId.contains("world")
                    || lowerId.contains("globe")
                        -> "Nature"

            // GENERAL (removed camera, photo, map, globe, music, video)
            lowerId.contains("settings") || lowerId.contains("gear")
                    || lowerId.contains("user") || lowerId.contains("person")
                    || lowerId.contains("home") || lowerId.contains("search")
                    || lowerId.contains("trash") || lowerId.contains("edit")
                    || lowerId.contains("heart") || lowerId.contains("star")
                    || lowerId.contains("fav") || lowerId.contains("tool")
                    || lowerId.contains("pin") || lowerId.contains("clock")
                    || lowerId.contains("time") || lowerId.contains("calendar")
                    || lowerId.contains("alarm") || lowerId.contains("bell")
                    || lowerId.contains("lock") || lowerId.contains("key")
                    || lowerId.contains("info") || lowerId.contains("help")
                    || lowerId.contains("message") || lowerId.contains("mail")
                    || lowerId.contains("call") || lowerId.contains("phone")
                    || lowerId.contains("light") || lowerId.contains("bulb")
                         -> "General"

            // ALL OTHER ICONS: move to "Other Symbols"
            else -> "Other Symbols"
        }
    }
}
