// app/src/main/java/com/example/currencyidentifier/ui/navigation/Screen.kt
package com.example.currencyidentifier.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home_screen")
    data object Camera : Screen("camera_screen")
    data object Result : Screen("result_screen") {
        // Companion object to create route with arguments if needed,
        // e.g., passing image URI to result screen (though we'll use ViewModel)
        // fun createRoute(imageUri: String) = "result_screen/$imageUri"
    }
}