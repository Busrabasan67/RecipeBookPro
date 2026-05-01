package com.recipebookpro.presentation.ui.cooking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.recipebookpro.R;
import com.recipebookpro.domain.model.Recipe;
import com.recipebookpro.domain.model.Step;
import com.recipebookpro.presentation.ui.BaseActivity;
import com.recipebookpro.presentation.ui.cooking.adapter.CookingStepAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CookingModeActivity extends BaseActivity implements TextToSpeech.OnInitListener {

    private Recipe recipe;
    private ViewPager2 vpCookingSteps;
    private TextView tvMicStatus;
    private View cardTimerOverlay;
    private TextView tvActiveTimer;
    private ImageButton btnTtsToggle;
    private FloatingActionButton fabMic;

    private TextToSpeech tts;
    private boolean isTtsEnabled = true;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private boolean isListening = false;
    private boolean isVoiceSessionActive = false;

    private CountDownTimer currentTimer;

    private final ActivityResultLauncher<String> requestMicPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startListening();
                } else {
                    Toast.makeText(this, R.string.mic_permission_denied, Toast.LENGTH_SHORT).show();
                    tvMicStatus.setText(R.string.mic_no_permission);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Keep screen awake while cooking
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_cooking_mode);

        applyInsetsToView(findViewById(R.id.llBottomControls));

        recipe = (Recipe) getIntent().getSerializableExtra("extra_recipe");
        if (recipe == null || recipe.getStepList().isEmpty()) {
            finish();
            return;
        }

        initViews();
        setupViewPager();
        initTTS();
        initSpeechRecognizer();

        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
    }

    private void initViews() {
        vpCookingSteps = findViewById(R.id.vpCookingSteps);
        tvMicStatus = findViewById(R.id.tvMicStatus);
        cardTimerOverlay = findViewById(R.id.cardTimerOverlay);
        tvActiveTimer = findViewById(R.id.tvActiveTimer);
        btnTtsToggle = findViewById(R.id.btnTtsToggle);
        fabMic = findViewById(R.id.fabMic);

        btnTtsToggle.setOnClickListener(v -> {
            isTtsEnabled = !isTtsEnabled;
            btnTtsToggle.setAlpha(isTtsEnabled ? 1.0f : 0.5f);
            if (!isTtsEnabled && tts != null) tts.stop();
        });

        fabMic.setOnClickListener(v -> {
            if (isVoiceSessionActive) {
                stopListeningSession();
            } else {
                checkMicAndListen();
            }
        });
    }

    private void setupViewPager() {
        CookingStepAdapter adapter = new CookingStepAdapter(recipe.getStepList(), this::startTimer);
        vpCookingSteps.setAdapter(adapter);

        vpCookingSteps.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                readStep(recipe.getStepList().get(position));
            }
        });
    }

    private void initTTS() {
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Locale trLocale = new Locale("tr", "TR");
            int result = tts.setLanguage(trLocale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault());
            }
            // Read the first step if possible
            vpCookingSteps.postDelayed(() -> {
                if (!recipe.getStepList().isEmpty()) {
                    readStep(recipe.getStepList().get(0));
                }
            }, 1000);
        } else {
            Toast.makeText(this, R.string.tts_init_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void readStep(Step step) {
        if (!isTtsEnabled || tts == null) return;
        tts.stop();
        tts.speak(step.getDescription(), TextToSpeech.QUEUE_FLUSH, null, "StepTTS");
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { tvMicStatus.setText(R.string.listening); }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() { tvMicStatus.setText(R.string.processing); }
                
                @Override
                public void onError(int error) {
                    isListening = false;
                    if (isVoiceSessionActive) {
                        tvMicStatus.setText(R.string.listening_restarting);
                        restartListeningWithDelay();
                    } else {
                        fabMic.setImageResource(R.drawable.ic_mic);
                        tvMicStatus.setText(R.string.listening_stopped);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    isListening = false;
                    tvMicStatus.setText(R.string.waiting);
                    
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        processVoiceCommand(matches.get(0).toLowerCase());
                    }
                    if (isVoiceSessionActive) {
                        restartListeningWithDelay();
                    } else {
                        fabMic.setImageResource(R.drawable.ic_mic);
                    }
                }

                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            tvMicStatus.setText(R.string.voice_recognition_unsupported);
            fabMic.setEnabled(false);
        }
    }

    private void checkMicAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startListeningSession();
        }
    }

    private void startListeningSession() {
        isVoiceSessionActive = true;
        startListening();
    }

    private void startListening() {
        if (!isVoiceSessionActive || isListening) return;
        if (speechRecognizer != null && recognizerIntent != null) {
            if (tts != null) tts.stop(); // Stop talking while listening
            speechRecognizer.startListening(recognizerIntent);
            isListening = true;
            fabMic.setImageResource(R.drawable.ic_volume_up); // Show some active state
            tvMicStatus.setText(R.string.listening);
        }
    }

    private void restartListeningWithDelay() {
        tvMicStatus.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed() && isVoiceSessionActive) {
                startListening();
            }
        }, 350);
    }

    private void stopListeningSession() {
        isVoiceSessionActive = false;
        stopListening();
    }

    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            isListening = false;
            fabMic.setImageResource(R.drawable.ic_mic);
            tvMicStatus.setText(R.string.waiting);
        }
    }

    private void processVoiceCommand(String command) {
        if (command.contains("sonraki")) {
            int current = vpCookingSteps.getCurrentItem();
            if (current < recipe.getStepList().size() - 1) vpCookingSteps.setCurrentItem(current + 1, true);
        } else if (command.contains("önceki")) {
            int current = vpCookingSteps.getCurrentItem();
            if (current > 0) vpCookingSteps.setCurrentItem(current - 1, true);
        } else if (command.contains("tekrarla") || command.contains("tekrar ok")) {
            int current = vpCookingSteps.getCurrentItem();
            readStep(recipe.getStepList().get(current));
        } else if (command.contains("başlat")) {
            int current = vpCookingSteps.getCurrentItem();
            Step step = recipe.getStepList().get(current);
            if (step.hasTimer()) startTimer(step.getTimerMinutes());
        } else if (command.contains("durdur")) {
            stopTimer();
        } else {
            Toast.makeText(this, getString(R.string.voice_command_not_understood, command), Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer(int minutes) {
        if (currentTimer != null) currentTimer.cancel();
        
        cardTimerOverlay.setVisibility(View.VISIBLE);
        long millis = minutes * 60 * 1000L;
        
        currentTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSecs = millisUntilFinished / 1000;
                long m = totalSecs / 60;
                long s = totalSecs % 60;
                tvActiveTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
            }

            @Override
            public void onFinish() {
                tvActiveTimer.setText(R.string.timer_zero);
                Toast.makeText(CookingModeActivity.this, R.string.timer_finished, Toast.LENGTH_LONG).show();
                if (isTtsEnabled && tts != null) {
                    tts.speak(getString(R.string.timer_finished), TextToSpeech.QUEUE_FLUSH, null, "TimerFinished");
                }
            }
        }.start();
    }

    private void stopTimer() {
        if (currentTimer != null) {
            currentTimer.cancel();
            currentTimer = null;
        }
        cardTimerOverlay.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isVoiceSessionActive = false;
        stopTimer();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        // Clear keep screen on flag just in case
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
