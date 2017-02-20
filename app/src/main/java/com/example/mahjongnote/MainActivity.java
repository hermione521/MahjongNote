package com.example.mahjongnote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int USER_NUM = 4;
    private static final ColorDrawable EDIT_TEXT_BACKGROUND_COLOR_STARTED =
            new ColorDrawable(Color.parseColor("#CCCCCC"));
    private static final ColorDrawable EDIT_TEXT_BACKGROUND_COLOR_PRESSED =
            new ColorDrawable(Color.parseColor("#FFFF88"));

    private static final int CALCULATE_DIAN_REQUEST = 0;
    private static final int CALCULATE_ZIMO_REQUEST = 1;

    private static WeakReference<Context> mContext;

    private static final List<EditText> editTextUsernames = new ArrayList<>(USER_NUM);
    private static final boolean[] usernamePressed = {false, false, false, false};
    private Button pressedEventButton = null;
    private EditText dianer = null;
    private static int winnerIndex = -1;

    private static final List<TextView> textViewScores = new ArrayList<>(USER_NUM);


    private static GameStatus gameStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = new WeakReference<Context>(this);
        setContentView(R.layout.activity_main);

        editTextUsernames.clear();
        editTextUsernames.add((EditText)findViewById(R.id.editText1));
        editTextUsernames.add((EditText)findViewById(R.id.editText2));
        editTextUsernames.add((EditText)findViewById(R.id.editText3));
        editTextUsernames.add((EditText)findViewById(R.id.editText4));

        textViewScores.clear();
        textViewScores.add((TextView)findViewById(R.id.eastScore));
        textViewScores.add((TextView)findViewById(R.id.southScore));
        textViewScores.add((TextView)findViewById(R.id.westScore));
        textViewScores.add((TextView)findViewById(R.id.northScore));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_CANCELED) {
            showSimpleAlert("Result not found", "We didn't receive any result requested.");
            return;
        }

        int result = intent.getIntExtra("result", -1);
        if (result == -1) {
            showSimpleAlert("Result error", "Cannot parse result to integer.");
            return;
        }

        // Plus changStick
        int changScore = gameStatus.getChangNum() * 300;

        // Dian
        if (requestCode == CALCULATE_DIAN_REQUEST) {
            gameStatus.updateScore(winnerIndex, result + changScore);
            gameStatus.updateScore(editTextUsernames.indexOf(dianer), -result - changScore);
        }
        // Zimo
        if (requestCode == CALCULATE_ZIMO_REQUEST) {
            // Zhuang wins
            if (winnerIndex == gameStatus.getZhuangIndex()) {
                int per = ScoreCalculator.div3(result);
                for (int i = 0; i < USER_NUM; ++i) {
                    gameStatus.updateScore(i, -per);
                }
                gameStatus.updateScore(winnerIndex, per * 4);
            } else { // Xian wins
                int half = ScoreCalculator.div2(result);
                int anotherHalf = ScoreCalculator.div2(half);
                gameStatus.updateScore(gameStatus.getZhuangIndex(), -half);
                for (int i = 0; i < USER_NUM; ++i) {
                    if (i != gameStatus.getZhuangIndex() && i != winnerIndex) {
                        gameStatus.updateScore(i, -anotherHalf);
                    }
                }
                gameStatus.updateScore(winnerIndex, anotherHalf * 2 + half);
            }
            // changStick score (not related to zhuang/xian)
            for (int i = 0; i < USER_NUM; ++i) {
                if (i != gameStatus.getZhuangIndex()) {
                    gameStatus.updateScore(i, -changScore / 3);
                } else {
                    gameStatus.updateScore(i, changScore);
                }
            }
        }
        // Plus lizhiStick
        gameStatus.updateScore(winnerIndex, gameStatus.getLizhiStickNum() * 1000);

        // Update game status
        gameStatus.setLizhiStickNum(0);
        if (winnerIndex == gameStatus.getZhuangIndex()) {
            gameStatus.increaseChangNum();
        } else {
            gameStatus.increaseChangName();
            gameStatus.setChangNum(0);
            gameStatus.increaseZhuangIndex();
            gameStatus.setCurrentPlayer(gameStatus.getZhuangIndex());
        }
        updateLizhiStickNum();
        updateChangFullName();
        updateScores();

        // Cleanup
        winnerIndex = -1;
        dianer = null;
        cancelEverything();

        Log.d("reuslt", gameStatus.toString());
    }

    public void startGame(View view) {
        // Check usernames non-empty and unique
        Set<String> set = new HashSet<>();
        boolean valid = true;
        for (EditText editText : editTextUsernames) {
            String text = editText.getText().toString();
            if (text.isEmpty() || set.contains(text)) {
                valid = false;
                break;
            }
            set.add(text);
        }
        if (!valid) {
            showSimpleAlert("Invalid username", "4 usernames should be non-empty and unique.");
            return;
        }

        // Make EditText behave like button
        for (EditText editText : editTextUsernames) {
            editText.setFocusable(false);
            editText.setClickable(true);
            editText.setCursorVisible(false);
            editText.setBackground(EDIT_TEXT_BACKGROUND_COLOR_STARTED);
            final Context that = this;
            editText.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    changedUsernamePressedOrNot(v);
                }
            });
        }
        hideKeyboard();

        // Hide start button and show other buttons
        findViewById(R.id.buttonStart).setVisibility(View.GONE);
        findViewById(R.id.buttonLizhi).setVisibility(View.VISIBLE);
        findViewById(R.id.buttonLiuju).setVisibility(View.VISIBLE);
        findViewById(R.id.buttonZimo).setVisibility(View.VISIBLE);
        findViewById(R.id.buttonDian).setVisibility(View.VISIBLE);
        findViewById(R.id.buttonConfirm).setVisibility(View.VISIBLE);

        // Clean state
        cancelEverything();
        dianer = null;

        // Game status
        gameStatus = new GameStatus();
        updateLizhiStickNum();
        updateChangFullName();
        updateScores();
    }

    /**
     * onclick event for buttonLizhi, buttonLiuju, buttonZimo
     */
    public void changeMahjongEventMode(View view) {
        // Reset dianer
        dianer = null;

        // Reset username "button"
        for (int i = 0; i < USER_NUM; ++i) {
            usernamePressed[i] = false;
            editTextUsernames.get(i).setBackground(EDIT_TEXT_BACKGROUND_COLOR_STARTED);
        }

        // Cancel if on the same button
        if (pressedEventButton == view) {
            pressedEventButton.setTextColor(Color.BLACK);
            pressedEventButton = null;
            return;
        }

        // Reset event button
        if (pressedEventButton != null) {
            pressedEventButton.setTextColor(Color.BLACK);
        }
        ((Button) view).setTextColor(Color.RED);
        pressedEventButton = (Button) view;
    }

    /**
     * onclick event for buttonDian (different because we need two users here)
     */
    public void changeMahjongEventModeDian(View view) {
        // If already clicked, cancel everything
        if (pressedEventButton == view) {
            cancelEverything();
            return;
        }

        // Check if only one uesr is clicked
        int clickedUserNum = 0;
        int dianerIndex = -1;
        for (int i = 0; i < USER_NUM; ++i) {
            if (usernamePressed[i]) {
                ++clickedUserNum;
                dianerIndex = i;
            }
            if (clickedUserNum > 1) {
                break;
            }
        }
        if (clickedUserNum != 1) {
            showSimpleAlert("Invalid", "必须选择恰好有一人点炮");
            return;
        }

        // This is a valid click, switch to mode Dian
        ((Button) view).setTextColor(Color.RED);
        pressedEventButton = (Button) view;
        dianer = editTextUsernames.get(dianerIndex);
    }

    /**
     * onclick event for editText1~4
     */
    public void changedUsernamePressedOrNot(View view) {
        if (dianer != null) {
            changedUsernamePressedOrNotModeDian(view);
            return;
        }
        int index = editTextUsernames.indexOf((EditText) view);
        if (usernamePressed[index]) {
            usernamePressed[index] = false;
            ((EditText) view).setBackground(EDIT_TEXT_BACKGROUND_COLOR_STARTED);
        } else {
            usernamePressed[index] = true;
            ((EditText) view).setBackground(EDIT_TEXT_BACKGROUND_COLOR_PRESSED);
        }
    }

    /**
     * onclick event for confirm button
     */
    public void confirm(View view) {
        if (pressedEventButton == null) {
            return;
        }
        switch (pressedEventButton.getId()) {
            case R.id.buttonDian:
                dianHappened();
                break;
            case R.id.buttonZimo:
                zimoHappened();
                break;
            case R.id.buttonLiuju:
                liujuHappened();
                break;
            case R.id.buttonLizhi:
                lizhiHappened();
                break;
        }
        Log.d("reuslt", gameStatus.toString());
    }

    // =================================================================================

    private void dianHappened() {
        // Find the winner
        winnerIndex = -1;
        for (int i = 0; i < USER_NUM; ++i) {
            if (usernamePressed[i] && editTextUsernames.get(i) != dianer) {
                winnerIndex = i;
                break;
            }
        }

        Intent intent = new Intent(this, CalculateActivity.class);
        intent.putExtra("zimo", false);
        intent.putExtra("zhuang_win", winnerIndex == gameStatus.getZhuangIndex());
        startActivityForResult(intent, CALCULATE_DIAN_REQUEST);
    }

    private void zimoHappened() {
        // Find the winner
        winnerIndex = -1;
        for (int i = 0; i < USER_NUM; ++i) {
            if (usernamePressed[i]) {
                winnerIndex = i;
                break;
            }
        }

        Intent intent = new Intent(this, CalculateActivity.class);
        intent.putExtra("zimo", true);
        intent.putExtra("zhuang_win", winnerIndex == gameStatus.getZhuangIndex());
        startActivityForResult(intent, CALCULATE_ZIMO_REQUEST);
    }

    private void lizhiHappened() {
        boolean updated = false;
        // Update game status
        for (int i = 0; i < USER_NUM; ++i) {
            if (usernamePressed[i]) {
                gameStatus.increaseLizhiStickNum();
                gameStatus.updateScore(i, -1000);
                updated = true;
            }
        }

        if (updated) {
            cancelEverything();
            updateLizhiStickNum();
            updateScores();
        }
    }

    private void liujuHappened() {
        // Update game status
        int tingPlayerNum = 0;
        for (int i = 0; i < USER_NUM; ++i) {
            if (usernamePressed[i]) {
                ++tingPlayerNum;
            }
        }
        if (tingPlayerNum > 0 || tingPlayerNum < USER_NUM) {
            for (int i = 0; i < USER_NUM; ++i) {
                if (usernamePressed[i]) {
                    gameStatus.updateScore(i, 3000 / tingPlayerNum);
                } else {
                    gameStatus.updateScore(i, -3000 / (USER_NUM - tingPlayerNum));
                }
            }
        }
        // Check zhuang
        if (usernamePressed[gameStatus.getZhuangIndex()]) {
            gameStatus.setChangNum(gameStatus.getChangNum() + 1);
        } else {
            gameStatus.increaseChangName();
            gameStatus.setChangNum(0);
            int nextZhuang = (gameStatus.getZhuangIndex() + 1) % USER_NUM;
            gameStatus.setZhuangIndex(nextZhuang);
            gameStatus.setCurrentPlayer(nextZhuang);
        }
        updateChangFullName();
        updateScores();

        cancelEverything();
    }

    // =================================================================================

    private void updateLizhiStickNum() {
        String text = getResources().getString(R.string.lizhiStickNumHint)
                + gameStatus.getLizhiStickNum();
        ((TextView) findViewById(R.id.lizhiStickNum)).setText(text);
    }

    private void updateChangFullName() {
        String text = gameStatus.getChangFullName();
        ((TextView) findViewById(R.id.changFullName)).setText(text);
    }

    private void updateScores() {
        int scores[] = gameStatus.getScores();
        for (int i = 0; i < USER_NUM; ++i) {
            textViewScores.get(i).setText(scores[i] + "");
        }
    }

    // =================================================================================

    private void changedUsernamePressedOrNotModeDian(View view) {
        // If dianer is clicked again, cancel everything
        if (view == dianer) {
            cancelEverything();
            dianer = null;
            return;
        }

        // If another user is also clicked (previously chosen winner), cancel it
        int anotherIndex = -1;
        for (int i = 0; i < USER_NUM; ++i) {
            if (usernamePressed[i] && editTextUsernames.get(i) != dianer) {
                anotherIndex = i;
                break;
            }
        }
        if (anotherIndex != -1) {
            usernamePressed[anotherIndex] = false;
            editTextUsernames.get(anotherIndex).setBackground(EDIT_TEXT_BACKGROUND_COLOR_STARTED);
        }

        // Make the new clicked user winner
        int winnerIndex = editTextUsernames.indexOf(view);
        usernamePressed[winnerIndex] = true;
        ((EditText) view).setBackground(EDIT_TEXT_BACKGROUND_COLOR_PRESSED);
    }

    private void cancelEverything() {
        for (int i = 0; i < USER_NUM; ++i) {
            usernamePressed[i] = false;
            editTextUsernames.get(i).setBackground(EDIT_TEXT_BACKGROUND_COLOR_STARTED);
        }
        if (pressedEventButton != null) {
            pressedEventButton.setTextColor(Color.BLACK);
        }
        pressedEventButton = null;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getRootView().getWindowToken(), 0);
    }

    private void showSimpleAlert(String title, String content) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(android.R.string.yes, null)
                .show();
    }

    public static Context getContext() {
        return mContext.get();
    }
}
