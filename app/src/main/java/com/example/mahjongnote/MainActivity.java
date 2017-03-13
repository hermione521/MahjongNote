package com.example.mahjongnote;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 10;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int FINISH_CHANG_NAME = 8;
    private static final int FINISH_LEAST_SCORE = 30000;

    private static final int USER_NUM = 4;
    private static final ColorDrawable EDIT_TEXT_BACKGROUND_COLOR_STARTED =
            new ColorDrawable(Color.parseColor("#CCCCCC"));
    private static final ColorDrawable EDIT_TEXT_BACKGROUND_COLOR_PRESSED =
            new ColorDrawable(Color.parseColor("#FFFF88"));

    private static final int REQUEST_CALCULATE_DIAN = 0;
    private static final int REQUEST_CALCULATE_ZIMO = 1;
    private static final int REQUEST_PICK_FILE = 2;

    private static WeakReference<Context> mContext;

    private static File outputDir = null;
    private static File outputStoryFile = null;
    private static File outputScoreFile = null;
    private static FileWriter outputStoryFileWriter = null;
    private static FileWriter outputScoreFileWriter = null;

    private static final List<EditText> editTextUsernames = new ArrayList<>(USER_NUM);
    private static final boolean[] usernamePressed = {false, false, false, false};
    private Button pressedEventButton = null;
    private EditText dianer = null;
    private static int winnerIndex = -1;

    private static final List<TextView> textViewScores = new ArrayList<>(USER_NUM);

    private static GameStatus gameStatus = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

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

        ((TextView) findViewById(R.id.allRecord)).setMovementMethod(new ScrollingMovementMethod());

        verifyStoragePermissions(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //File picker
        if (requestCode == REQUEST_PICK_FILE) {
            if (resultCode == RESULT_OK) {
                loadGameWithScoreFile(intent.getStringExtra(FilePickerActivity.RESULT_FILE_PATH));
            }
            return;
        }

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

        String csvString = gameStatus.getChangFullName() + ",";

        // Dian
        if (requestCode == REQUEST_CALCULATE_DIAN) {
            gameStatus.updateScore(winnerIndex, result + changScore);
            gameStatus.updateScore(editTextUsernames.indexOf(dianer), -result - changScore);

            csvString += String.format(Locale.getDefault(), "%s,%s(%s),%d",
                    getString(R.string.dian),
                    editTextUsernames.get(winnerIndex).getText(),
                    dianer.getText(),
                    result);
        }
        // Zimo
        if (requestCode == REQUEST_CALCULATE_ZIMO) {
            int total = 0;
            // Zhuang wins
            if (winnerIndex == gameStatus.getZhuangIndex()) {
                int per = ScoreCalculator.div3(result);
                for (int i = 0; i < USER_NUM; ++i) {
                    gameStatus.updateScore(i, -per);
                }
                gameStatus.updateScore(winnerIndex, per * 4);
                total = per * 3;
            } else { // Xian wins
                int half = ScoreCalculator.div2(result);
                int anotherHalf = ScoreCalculator.div2(half);
                gameStatus.updateScore(gameStatus.getZhuangIndex(), -half);
                for (int i = 0; i < USER_NUM; ++i) {
                    if (i != gameStatus.getZhuangIndex() && i != winnerIndex) {
                        gameStatus.updateScore(i, -anotherHalf);
                    }
                }
                total = anotherHalf * 2 + half;
                gameStatus.updateScore(winnerIndex, total);
            }
            // changStick score (not related to zhuang/xian)
            for (int i = 0; i < USER_NUM; ++i) {
                if (i != winnerIndex) {
                    gameStatus.updateScore(i, -changScore / 3);
                } else {
                    gameStatus.updateScore(i, changScore);
                }
            }

            csvString += String.format(Locale.getDefault(), "%s,%s,%d",
                    getString(R.string.zimo),
                    editTextUsernames.get(winnerIndex).getText(),
                    total);
        }
        // Plus lizhiStick
        gameStatus.updateScore(winnerIndex, gameStatus.getLizhiStickNum() * 1000);

        // Record
        updateOutput(outputStoryFileWriter, csvString + intent.getStringExtra("csv"));
        updateOutput(outputScoreFileWriter, gameStatus.getCsvString());

        // Update game status
        gameStatus.setLizhiStickNum(0);
        if (winnerIndex == gameStatus.getZhuangIndex()) {
            gameStatus.increaseChangNum();
        } else {
            gameStatus.increaseChangName();
            gameStatus.setChangNum(0);
            gameStatus.increaseZhuangIndex();
        }
        updateLizhiStickNum();
        updateChangFullName();
        updateScores();

        // Cleanup
        winnerIndex = -1;
        dianer = null;
        cancelEverything();

        finishGameIfShould();

        Log.d("reuslt", gameStatus.toString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.loadHistory:
                loadHistory();
                return true;
            case R.id.finish:
                finishGame();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * onClick event for buttonStart
     */
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

        // Get public external directory and files for writing
        if (!isExternalStorageWritable()) {
            showSimpleAlert("External storage not writable", "And we just want to write there!");
            return;
        }
        outputDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getString(R.string.app_name));
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            showSimpleAlert("Failed to make output directory", "We are not able to record this game.");
            return;
        }
        String now = new SimpleDateFormat("yyyy-dd-MM_HH:mm:ss").format(new Date());
        outputStoryFile = new File(outputDir, getString(R.string.app_name) + "_record_" + now + ".csv");
        outputScoreFile = new File(outputDir, getString(R.string.app_name) + "_score_" + now + ".csv");
        try {
            outputStoryFileWriter = new FileWriter(outputStoryFile, true);
            outputScoreFileWriter = new FileWriter(outputScoreFile, true);
            updateOutput(outputStoryFileWriter, "round,event,players and remarks");
            String scoreHeader = "round";
            for (EditText editText : editTextUsernames) {
                scoreHeader += "," + editText.getText();
            }
            updateOutput(outputScoreFileWriter, scoreHeader);
        } catch (IOException e) {
            showSimpleAlert("Failed to create files", e.getMessage());
            return;
        }

        // Game status
        gameStatus = new GameStatus();

        startGameUI();
    }

    private void startGameUI() {
        // Make EditText behave like button
        for (EditText editText : editTextUsernames) {
            editText.setFocusable(false);
            editText.setClickable(true);
            editText.setCursorVisible(false);
            editText.setBackground(EDIT_TEXT_BACKGROUND_COLOR_STARTED);
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

        // Game data
        updateLizhiStickNum();
        updateChangFullName();
        updateScores();
    }

    /**
     * onClick event for buttonLizhi, buttonLiuju, buttonZimo
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
     * onClick event for buttonDian (different because we need two users here)
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
     * onClick event for editText1~4
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
     * onClick event for confirm button
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

    private void loadHistory() {
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(REQUEST_PICK_FILE)
                .withFilter(Pattern.compile(getString(R.string.app_name) + "_score_.*\\.csv$"))
                .withHiddenFiles(true)
                .start();
    }

    private void loadGameWithScoreFile(String scoreFilePath) {
        String content = null;
        try {
            content = new Scanner(new File(scoreFilePath)).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            showSimpleAlert("Cannot read score file", scoreFilePath);
        }
        String recordPath = scoreFilePath.replace("_score_", "_record_");
        String contentRecord = null;
        try {
            contentRecord = new Scanner(new File(recordPath)).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            showSimpleAlert("Cannot read record file", recordPath);
        }

        if (content.indexOf("\n") == -1) {
            showSimpleAlert("No game found", "You haven't started a single game. No need to load.");
        }
        String firstLine = content.substring(0, content.indexOf("\n"));
        String lastLine = content.substring(content.lastIndexOf("\n"), content.length());
        String recordLastLine = contentRecord.substring(content.lastIndexOf("\n"), content.length());
        String[] firstLineSplit = firstLine.split(",");
        String[] lastLineSplit = lastLine.split(",");
//        Log.d("tag", lastLine.length() + "");
//        Log.d("tag", lastLine.indexOf(getString(R.string.ju)) + "");

        // File and FileWriter
        outputStoryFile = new File(scoreFilePath);
        outputScoreFile = new File(recordPath);
        try {
            outputStoryFileWriter = new FileWriter(outputStoryFile, true);
            outputScoreFileWriter = new FileWriter(outputScoreFile, true);
        } catch (IOException e) {
            showSimpleAlert("Failed to create files", e.getMessage());
            return;
        }

        // Players
        for (int i = 0; i < USER_NUM; ++i) {
            editTextUsernames.get(i).setText(firstLineSplit[i + 1]);
        }

        // Game status
        List<Integer> scores = new ArrayList<>(4);
        for (int i = 1; i < 5; ++i) {
            scores.add(Integer.parseInt(lastLineSplit[i]));
        }
        gameStatus = new GameStatus(
                recordLastLine,
                Arrays.asList(Arrays.copyOfRange(firstLineSplit, 1, USER_NUM)),
                scores);

        // Start now!
        startGameUI();
    }

    private void finishGame() {
        new AlertDialog.Builder(this)
                .setTitle("Finish game")
                .setMessage("Are you sure to finish this game?")
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            outputScoreFileWriter.close();
                            outputStoryFileWriter.close();
                        } catch (IOException e) {
                            // Don't even care
                        }
                        dialog.cancel();
                        showSimpleAlert("Game finished",
                                "Output file: "
                                + outputStoryFile.getAbsolutePath() + ", "
                                + outputScoreFile.getAbsolutePath());
                    }
                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }

    private void finishGameIfShould() {
        if (gameStatus.getChangName() < FINISH_CHANG_NAME) {
            return;
        }
        // Highest score
        int highest = Collections.max(gameStatus.getScores());
        if (highest >= FINISH_LEAST_SCORE) {
            finishGame();
        }
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
        startActivityForResult(intent, REQUEST_CALCULATE_DIAN);
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
        startActivityForResult(intent, REQUEST_CALCULATE_ZIMO);
    }

    private void lizhiHappened() {
        boolean updated = false;
        String lizhiPlayerCsv = "";
        // Update game status
        for (int i = 0; i < USER_NUM; ++i) {
            if (usernamePressed[i]) {
                gameStatus.increaseLizhiStickNum();
                gameStatus.updateScore(i, -1000);
                lizhiPlayerCsv += "," + editTextUsernames.get(i).getText();
                updated = true;
            }
        }

        if (updated) {
            cancelEverything();
            updateLizhiStickNum();
            updateScores();

            updateOutput(outputStoryFileWriter,
                    gameStatus.getChangFullName() + "," + getString(R.string.lizhi) + lizhiPlayerCsv);
            // Don't update outputScoreFile here
        }
    }

    private void liujuHappened() {
        // Update game status
        int tingPlayerNum = 0;
        String tingPlayerCsv = "";
        for (int i = 0; i < USER_NUM; ++i) {
            if (usernamePressed[i]) {
                ++tingPlayerNum;
                tingPlayerCsv += "," + editTextUsernames.get(i).getText();
            }
        }
        if (tingPlayerNum > 0 && tingPlayerNum < USER_NUM) {
            for (int i = 0; i < USER_NUM; ++i) {
                if (usernamePressed[i]) {
                    gameStatus.updateScore(i, 3000 / tingPlayerNum);
                } else {
                    gameStatus.updateScore(i, -3000 / (USER_NUM - tingPlayerNum));
                }
            }
        }

        // Record
        updateOutput(outputScoreFileWriter, gameStatus.getCsvString());
        updateOutput(outputStoryFileWriter,
                gameStatus.getChangFullName() + "," + getString(R.string.liuju) + tingPlayerCsv);

        // Check zhuang
        if (!usernamePressed[gameStatus.getZhuangIndex()]) {
            gameStatus.increaseChangName();
            int nextZhuang = (gameStatus.getZhuangIndex() + 1) % USER_NUM;
            gameStatus.setZhuangIndex(nextZhuang);
        }
        gameStatus.setChangNum(gameStatus.getChangNum() + 1);
        updateChangFullName();
        updateScores();

        cancelEverything();

        finishGameIfShould();
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
        List<Integer> scores = gameStatus.getScores();
        for (int i = 0; i < USER_NUM; ++i) {
            textViewScores.get(i).setText(scores.get(i) + "");
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

    private void updateOutput(FileWriter writer, String str) {
        try {
            writer.write(str + "\n");
            writer.flush();
            ((TextView) findViewById(R.id.allRecord)).append(str + "\n");
        } catch (IOException e) {
            showSimpleAlert("Failed to write record", str);
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static Context getContext() {
        return mContext.get();
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
