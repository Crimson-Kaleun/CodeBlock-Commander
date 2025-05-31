import android.app.Activity
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.system.exitProcess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


@Composable
fun MainMenu(navController: NavController) {

    val scope = rememberCoroutineScope()
    var visibleText by remember { mutableStateOf(false) }
    var visibleButtons by remember { mutableStateOf(false) }
    val activity = (LocalContext.current as? Activity)
    LaunchedEffect(Unit) {
        delay(100)
        visibleText = true
        delay(1000)
        visibleButtons = true
    }
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
        AnimatedVisibility(
            visible = visibleText,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(1000))
        ) {
            Text(
                text = "КодСтрой",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
        }
        AnimatedVisibility(
            visible = visibleButtons,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(1000))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        //Log.v("123", "123");
                        scope.launch {
                            visibleButtons = false
                            visibleText = false
                            delay(1000)
                            navController.navigate("WorkPlace")
                        }
                    }
                ){
                    Text("Начать строить!")
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { activity?.finish() }) {
                    Text("Выйти")
                }
            }
        }
    }
}
