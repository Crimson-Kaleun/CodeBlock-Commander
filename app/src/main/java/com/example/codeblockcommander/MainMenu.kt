import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.system.exitProcess


@Composable
fun MainMenu(navController: NavController) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFAAE59C), Color(0xFF4D247F))
            )
        )
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                //Log.v("123", "123");
                navController.navigate("WorkPlace")
                      },
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Начать строить!")
        }

        Button(
            onClick = {
                exitProcess(0)
            },
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Выйти")
        }

    }
}