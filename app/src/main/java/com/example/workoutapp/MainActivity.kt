package com.example.workoutapp

import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import kotlin.random.Random


data class Exercise(
    val name: String,
    val description: String,
    val imageRes: Int
)

class WorkoutViewModel : ViewModel() {

    private val exerciseTime = 30
    private val pauseTime = 10

    val exercises = listOf(
        Exercise("Liegestütze", "Trainiert Brust, Schultern und Arme.", R.drawable.pushup),
        Exercise("Kniebeugen", "Trainiert Beine und Po.", R.drawable.squat),
        Exercise("Plank", "Stärkt Bauch, Rücken und Oberkörper.", R.drawable.plank)
    )

    val currentExerciseIndex = MutableLiveData(0)
    val secondsLeft = MutableLiveData(exerciseTime)
    val isPause = MutableLiveData(false)
    val isRunning = MutableLiveData(false)
    val workoutFinished = MutableLiveData(false)

    private var timer: CountDownTimer? = null

    // Startet das Workout und den ersten Timer
    fun startWorkout(context: Context) {
        if (isRunning.value == true) return
        isRunning.value = true
        workoutFinished.value = false
        playSound(context, R.raw.start)
        startTimer(context, exerciseTime)
    }

    private fun startTimer(context: Context, duration: Int) {
        timer?.cancel()
        secondsLeft.value = duration

        timer = object : CountDownTimer(duration * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                secondsLeft.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                nextStep(context)
            }
        }.start()
    }

    private fun nextStep(context: Context) {
        val index = currentExerciseIndex.value ?: 0

        if (isPause.value == false) {
            playSound(context, R.raw.end)

            if (index == exercises.lastIndex) {
                finishWorkout(context)
            } else {
                isPause.value = true
                playSound(context, R.raw.pause)
                startTimer(context, pauseTime)
            }
        } else {
            isPause.value = false
            currentExerciseIndex.value = index + 1
            playSound(context, R.raw.start)
            startTimer(context, exerciseTime)
        }
    }

    private fun finishWorkout(context: Context) {
        timer?.cancel()
        isRunning.value = false
        workoutFinished.value = true

        val prefs = context.getSharedPreferences("workout_progress", Context.MODE_PRIVATE)
        val completed = prefs.getInt("completed_workouts", 0)
        prefs.edit().putInt("completed_workouts", completed + 1).apply()

        playSound(context, R.raw.end)
    }

    fun resetWorkout() {
        timer?.cancel()
        currentExerciseIndex.value = 0
        secondsLeft.value = exerciseTime
        isPause.value = false
        isRunning.value = false
        workoutFinished.value = false
    }

    // Spielt einen Signalton ab
    private fun playSound(context: Context, soundRes: Int) {
        val player = MediaPlayer.create(context, soundRes)
        player.setOnCompletionListener {
            it.release()
        }
        player.start()
    }

    override fun onCleared() {
        timer?.cancel()
        super.onCleared()
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<WorkoutViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                WorkoutScreen(viewModel)
            }
        }
    }
}

@Composable
fun WorkoutScreen(viewModel: WorkoutViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val currentIndex by viewModel.currentExerciseIndex.observeAsState(0)
    val secondsLeft by viewModel.secondsLeft.observeAsState(30)
    val isPause by viewModel.isPause.observeAsState(false)
    val isRunning by viewModel.isRunning.observeAsState(false)
    val workoutFinished by viewModel.workoutFinished.observeAsState(false)

    val exercise = viewModel.exercises[currentIndex]
    val progress = (currentIndex + if (isPause) 1 else 0).toFloat() / viewModel.exercises.size

    val prefs = context.getSharedPreferences("workout_progress", Context.MODE_PRIVATE)
    val completedWorkouts = prefs.getInt("completed_workouts", 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Workout-App",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Fortschritt: ${currentIndex + 1} von ${viewModel.exercises.size} Übungen")

        Spacer(modifier = Modifier.height(24.dp))

        if (workoutFinished) {
            Text(
                text = "Workout abgeschlossen!",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Abgeschlossene Workouts: ${completedWorkouts + 1}")
        } else if (isPause) {
            Text(
                text = "Pause",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$secondsLeft Sekunden",
                style = MaterialTheme.typography.headlineMedium
            )
        } else {
            Image(
                painter = painterResource(id = exercise.imageRes),
                contentDescription = exercise.name,
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = exercise.name,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(exercise.description)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "$secondsLeft Sekunden",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.startWorkout(context) },
            enabled = !isRunning && !workoutFinished
        ) {
            Text("Workout starten")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.resetWorkout() }
        ) {
            Text("Zurücksetzen")
        }
    }
}