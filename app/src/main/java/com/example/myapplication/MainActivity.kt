package com.example.myapplication

//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.tooling.preview.Preview
//import com.example.myapplication.ui.theme.DementiaTheme
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            DementiaTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    DementiaTheme {
//        Greeting("Android")
//    }
//}


//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import com.evoke.fyp.ui.theme.GreenTheme
//import com.example.myapplication.ui.PersonFormContent  // ← composable
//import com.example.myapplication.ui.PersonFormMode     // ← enum
//import com.example.myapplication.ui.NotRecognisedContent
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        window.navigationBarColor = android.graphics.Color.parseColor("#2D5A3D")
//        setContent {
//            GreenTheme {
//                NotRecognisedContent()
//            }
//        }
//    }
//}


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsControllerCompat
import com.example.myapplication.ui.FigmaMotionApp
import com.example.myapplication.ui.navigation.AppNavigation
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.parseColor("#3E634F")
            ),
            navigationBarStyle = SystemBarStyle.dark(
                android.graphics.Color.parseColor("#3E634F")
            )
        )


            setContent {
                AppNavigation()

            }


        }
    }






