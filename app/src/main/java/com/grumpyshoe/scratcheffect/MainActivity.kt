package com.grumpyshoe.scratcheffect

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grumpyshoe.scratchable.ScratchableImage
import com.grumpyshoe.scratcheffect.ui.theme.ScratchEffectTheme

class MainActivity : ComponentActivity() {

    private val viewModel = MainViewModel()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScratchEffectTheme {
                Scaffold {

                    val answerList by viewModel.answerList.collectAsState()
                    val gameState by viewModel.state.collectAsState()

                    Box(
                        modifier = Modifier
                            .padding(it)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        ScratchGameView(
                            answerList = answerList,
                            gameState = gameState,
                            onScratch = viewModel::onScratch,
                            reset = viewModel::reset
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScratchGameView(
    answerList: List<Boolean>,
    gameState: GameState,
    onScratch: (Int, Boolean) -> Unit,
    reset: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = "Scratch it!",
            style = MaterialTheme.typography.displayLarge
        )

        Text(
            text = "3 check signs and you win!",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (gameState) {
            GameState.RUNNING -> ScratchField(
                answerList = answerList,
                onScratch = onScratch
            )

            GameState.WIN -> WinnerScreen(
                reset = reset
            )

            GameState.LOOSE -> LooserScreen(
                reset = reset
            )
        }

    }
}

@Composable
fun ScratchField(
    answerList: List<Boolean>,
    onScratch: (Int, Boolean) -> Unit,
) {

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        itemsIndexed(answerList) { index, isWinField ->

            Box(
                modifier = Modifier,
                contentAlignment = Alignment.Center
            ) {

                ScratchableImage(
                    modifier = Modifier
                        .clip(CircleShape)
                        .aspectRatio(1f),
                    image = isWinField.let { fieldValue ->
                        when (fieldValue) {
                            true -> R.drawable.check
                            else -> R.drawable.blank
                        }
                    },
                    eraserRadius = 15.dp,
                    onTouch = {
                        onScratch(index, isWinField)
                    }
                )
            }
        }
    }
}


@Composable
fun WinnerScreen(reset: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\uD83E\uDD73 WINNER \uD83E\uDD73",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { reset() }) {
                Text(text = "Try Again")
            }
        }
    }
}

@Composable
fun LooserScreen(reset: () -> Unit) {

    Box() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\uD83D\uDE43 Sorry, maybe\nnext time!",
                style = MaterialTheme.typography.displayMedium.copy(
                    platformStyle = PlatformTextStyle(
                        emojiSupportMatch = EmojiSupportMatch.Default
                    )
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { reset() }) {
                Text(text = "Try Again")
            }
        }
    }
}

@Preview
@Composable
private fun RunningState() {
    MaterialTheme {
        ScratchGameView(
            answerList = List(9) { (0..1).random() == 1 },
            gameState = GameState.RUNNING,
            onScratch = { _, _ -> },
            reset = {}
        )
    }
}

@Preview
@Composable
private fun WinnerState() {
    MaterialTheme {
        ScratchGameView(
            answerList = List(9) { (0..1).random() == 1 },
            gameState = GameState.WIN,
            onScratch = { _, _ -> },
            reset = {}
        )
    }
}

@Preview
@Composable
private fun LooserState() {
    MaterialTheme {
        ScratchGameView(
            answerList = List(9) { (0..1).random() == 1 },
            gameState = GameState.LOOSE,
            onScratch = { _, _ -> },
            reset = {}
        )
    }
}