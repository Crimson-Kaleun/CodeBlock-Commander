import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.system.exitProcess

@Composable
fun MainMenu(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Главное меню", modifier = Modifier.padding(32.dp))

        Button(
            onClick = {
                Log.v("123", "123");
                navController.navigate("WorkPlace")
                      },
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Перейти к программе")
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