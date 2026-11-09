package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.ScoreboardScreen
import com.example.ui.theme.BillardCaramboleTheme
import com.example.viewmodel.ScoreboardViewModel
import com.example.viewmodel.ScoreboardViewModelFactory

class MainActivity : ComponentActivity() {

    private val scoreboardViewModel: ScoreboardViewModel by viewModels {
        ScoreboardViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Garder l'écran tactile du billard toujours allumé pendant les matchs
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            BillardCaramboleTheme {
                ScoreboardScreen(viewModel = scoreboardViewModel)
            }
        }
    }

    /**
     * Interception globale des événements Télécommande Bluetooth HID (Clavier/Présentateur)
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && scoreboardViewModel.handleRemoteKeyEvent(event)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
