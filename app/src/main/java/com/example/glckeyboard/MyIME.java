package com.example.glckeyboard;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Locale;

import android.inputmethodservice.InputMethodService;

/**
 * Physical-keyboard-focused IME for Android 13+.
 */
public final class MyIME extends InputMethodService {

    private enum LanguageMode {
        JAPANESE("あ"),
        ENGLISH("A"),
        GALACTIC("⨅");

        private final String marker;

        LanguageMode(String marker) {
            this.marker = marker;
        }

        @NonNull
        String marker() {
            return marker;
        }

        @NonNull
        LanguageMode next() {
            return switch (this) {
                case JAPANESE -> ENGLISH;
                case ENGLISH -> GALACTIC;
                case GALACTIC -> JAPANESE;
            };
        }
    }

    private static final int GAL_BASE = 0x2A05;
    private static final int OVERLAY_DURATION_MS = 1800;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LanguageMode currentMode = LanguageMode.JAPANESE;

    private WindowManager windowManager;
    private View overlayView;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = getSystemService(WindowManager.class);
    }

    @Override
    public boolean onEvaluateInputViewShown() {
        return !isPhysicalKeyboardConnected();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event != null && event.isCtrlPressed() && keyCode == KeyEvent.KEYCODE_SPACE) {
            cycleMode();
            return true;
        }

        if (currentMode == LanguageMode.GALACTIC && isAlphabetKey(keyCode)) {
            final String galChar = mapAlphabetToGalactic(keyCode);
            if (!TextUtils.isEmpty(galChar)) {
                final InputConnection connection = getCurrentInputConnection();
                if (connection != null) {
                    connection.commitText(galChar, 1);
                    return true;
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void cycleMode() {
        currentMode = currentMode.next();
        showModeOverlay(currentMode.marker());
    }

    private boolean isPhysicalKeyboardConnected() {
        final Configuration config = getResources().getConfiguration();
        final boolean hasHardwareKeys = config.keyboard != Configuration.KEYBOARD_NOKEYS;
        final boolean hardKeyboardOpen = config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
        return hasHardwareKeys && hardKeyboardOpen;
    }

    private boolean isAlphabetKey(int keyCode) {
        return keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z;
    }

    @NonNull
    private String mapAlphabetToGalactic(int keyCode) {
        final int index = keyCode - KeyEvent.KEYCODE_A;
        final int codePoint = GAL_BASE + index;
        return new String(Character.toChars(codePoint));
    }

    private void showModeOverlay(@NonNull String label) {
        if (windowManager == null) {
            return;
        }

        removeOverlayIfNeeded();

        final TextView badge = new TextView(this);
        badge.setText(label);
        badge.setTextSize(56f);
        badge.setTextColor(Color.WHITE);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(0xA6000000);
        badge.setPadding(56, 40, 56, 40);
        badge.setAllCaps(false);
        badge.setTextLocale(Locale.JAPANESE);

        final int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;

        try {
            windowManager.addView(badge, params);
            overlayView = badge;
            mainHandler.postDelayed(this::removeOverlayIfNeeded, OVERLAY_DURATION_MS);
        } catch (SecurityException ignored) {
            // Overlay permission may be denied by OEM policy.
        }
    }

    private void removeOverlayIfNeeded() {
        if (overlayView == null || windowManager == null) {
            return;
        }
        try {
            windowManager.removeViewImmediate(overlayView);
        } catch (IllegalArgumentException ignored) {
            // Already removed.
        }
        overlayView = null;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        removeOverlayIfNeeded();
        super.onDestroy();
    }
}
