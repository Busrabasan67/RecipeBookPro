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
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

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
    private MaterialButton btnTtsToggle;
    private FloatingActionButton fabMic;

    private TextToSpeech tts;
    private boolean isTtsEnabled = true;
    private boolean isFirstReadAttempted = false;

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
        checkMicAndListen();
        setupDraggableTimer();

        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
    }

    private void initViews() {
        vpCookingSteps = findViewById(R.id.vpCookingSteps);
        tvMicStatus = findViewById(R.id.tvMicStatus);
        cardTimerOverlay = findViewById(R.id.cardTimerOverlay);
        tvActiveTimer = findViewById(R.id.tvActiveTimer);
        btnTtsToggle = findViewById(R.id.btnTtsToggle);
        fabMic = findViewById(R.id.fabMic);
        MaterialButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnTtsToggle.setOnClickListener(v -> {
            isTtsEnabled = !isTtsEnabled;
            btnTtsToggle.setIconTint(ContextCompat.getColorStateList(this, 
                isTtsEnabled ? R.color.md_theme_light_primary : R.color.md_theme_light_outline));
            btnTtsToggle.setAlpha(isTtsEnabled ? 1.0f : 0.6f);
            if (!isTtsEnabled && tts != null) tts.stop();
            else if (isTtsEnabled) readStep(recipe.getStepList().get(vpCookingSteps.getCurrentItem()));
        });

        fabMic.setOnClickListener(v -> {
            if (isVoiceSessionActive) {
                stopListeningSession();
            } else {
                checkMicAndListen();
            }
        });
    }

    private void setupDraggableTimer() {
        cardTimerOverlay.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        view.setX(event.getRawX() + dX);
                        view.setY(event.getRawY() + dY);
                        break;
                    default:
                        return false;
                }
                return true;
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
                Step step = recipe.getStepList().get(position);
                readStep(step);
                updateTimerForStep(step);
            }
        });
    }

    private void initTTS() {
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            String currentLang = Locale.getDefault().getLanguage();
            Locale targetLocale = currentLang.equalsIgnoreCase("en") ? Locale.ENGLISH : new Locale("tr", "TR");
            tts.setLanguage(targetLocale);
            
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {
                    runOnUiThread(() -> {
                        if (isVoiceSessionActive) stopListening();
                    });
                }
                
                @Override public void onDone(String utteranceId) {
                    runOnUiThread(() -> {
                        if (isVoiceSessionActive) startListening();
                    });
                }
                
                @Override public void onError(String utteranceId) {
                    Log.e("TTS", "Error in TTS: " + utteranceId);
                    runOnUiThread(() -> {
                        if (isVoiceSessionActive) startListening();
                    });
                }
            });

            // Read the first step if it hasn't been read yet by the ViewPager callback
            vpCookingSteps.postDelayed(() -> {
                if (!recipe.getStepList().isEmpty() && !isFirstReadAttempted) {
                    Step currentStep = recipe.getStepList().get(vpCookingSteps.getCurrentItem());
                    readStep(currentStep);
                    updateTimerForStep(currentStep);
                }
            }, 500);
        } else {
            Toast.makeText(this, R.string.tts_init_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void readStep(Step step) {
        if (!isTtsEnabled || tts == null || step == null) return;
        isFirstReadAttempted = true;
        
        String textToSpeak = step.getDisplayDescription();
        if (textToSpeak == null || textToSpeak.trim().isEmpty()) return;

        // Force stop any current speech to avoid overlap
        tts.stop();
        
        // Refined language detection
        String currentLang = Locale.getDefault().getLanguage();
        if (step.getTranslatedDescription() != null && !step.getTranslatedDescription().isEmpty()) {
            tts.setLanguage(Locale.ENGLISH);
        } else {
            tts.setLanguage(currentLang.equalsIgnoreCase("en") ? Locale.ENGLISH : new Locale("tr", "TR"));
        }

        // Use a slightly longer delay to ensure the UI has settled if coming from a page change
        vpCookingSteps.postDelayed(() -> {
            if (isTtsEnabled && tts != null) {
                Bundle params = new Bundle();
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Step_" + step.getOrder());
                tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "Step_" + step.getOrder());
            }
        }, 100);
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            
            String currentLang = Locale.getDefault().getLanguage();
            String speechLang = currentLang.equalsIgnoreCase("en") ? "en-US" : "tr-TR";
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLang);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { tvMicStatus.setText(R.string.listening); }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() { tvMicStatus.setText(R.string.processing); }
                
                @Override
                public void onError(int error) {
                    isListening = false;
                    Log.d("Voice", "Error code: " + error);
                    if (isVoiceSessionActive) {
                        // Error 7 is timeout, error 6 is no speech, etc.
                        // We restart for most errors to keep the session alive.
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
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.permission_required)
                    .setMessage(R.string.mic_permission_rationale)
                    .setPositiveButton(R.string.accept, (dialog, which) -> {
                        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
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
            speechRecognizer.startListening(recognizerIntent);
            isListening = true;
            fabMic.setImageResource(R.drawable.ic_volume_up); // Show some active state
            tvMicStatus.setText(R.string.listening);
        }
    }

    private void restartListeningWithDelay() {
        if (tts != null && tts.isSpeaking()) return; // Don't restart if TTS is active
        tvMicStatus.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed() && isVoiceSessionActive) {
                startListening();
            }
        }, 500);
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
        if (command.contains("sonraki") || command.contains("next")) {
            int current = vpCookingSteps.getCurrentItem();
            if (current < recipe.getStepList().size() - 1) {
                vpCookingSteps.setCurrentItem(current + 1, true);
            } else {
                if (isTtsEnabled && tts != null) {
                    tts.speak(getString(R.string.recipe_finished), TextToSpeech.QUEUE_FLUSH, null, "RecipeFinished");
                }
                Toast.makeText(this, R.string.recipe_finished, Toast.LENGTH_LONG).show();
                // Return to details automatically
                vpCookingSteps.postDelayed(this::finish, 3000);
            }
        } else if (command.contains("önceki") || command.contains("previous") || command.contains("back")) {
            int current = vpCookingSteps.getCurrentItem();
            if (current > 0) vpCookingSteps.setCurrentItem(current - 1, true);
        } else if (command.contains("tekrarla") || command.contains("repeat") || command.contains("again")) {
            int current = vpCookingSteps.getCurrentItem();
            readStep(recipe.getStepList().get(current));
        } else if (command.contains("başlat") || command.contains("start")) {
            int current = vpCookingSteps.getCurrentItem();
            Step step = recipe.getStepList().get(current);
            if (step.hasTimer()) startTimer(step.getTimerMinutes());
        } else if (command.contains("durdur") || command.contains("stop") || command.contains("cancel")) {
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
                String msg = getString(R.string.timer_finished) + " " + getString(R.string.step_finished);
                Toast.makeText(CookingModeActivity.this, msg, Toast.LENGTH_LONG).show();
                
                if (isTtsEnabled && tts != null) {
                    tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "TimerFinished");
                }

                // Auto-advance to next step if possible
                vpCookingSteps.postDelayed(() -> {
                    int current = vpCookingSteps.getCurrentItem();
                    if (current < recipe.getStepList().size() - 1) {
                        vpCookingSteps.setCurrentItem(current + 1, true);
                    } else {
                        // All steps done via timer
                        if (isTtsEnabled && tts != null) {
                            tts.speak(getString(R.string.recipe_finished), TextToSpeech.QUEUE_FLUSH, null, "RecipeFinished");
                        }
                        Toast.makeText(CookingModeActivity.this, R.string.recipe_finished, Toast.LENGTH_LONG).show();
                        vpCookingSteps.postDelayed(CookingModeActivity.this::finish, 3000);
                    }
                }, 2000); // 2 second delay to allow hearing the completion message
            }
        }.start();
    }

    private void updateTimerForStep(Step step) {
        if (step != null && step.hasTimer()) {
            if (currentTimer != null) {
                currentTimer.cancel();
                currentTimer = null;
            }
            cardTimerOverlay.setVisibility(View.VISIBLE);
            int minutes = step.getTimerMinutes();
            tvActiveTimer.setText(String.format(Locale.getDefault(), "%02d:00", minutes));
        } else {
            // Hide timer if step doesn't have one
            stopTimer();
        }
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
