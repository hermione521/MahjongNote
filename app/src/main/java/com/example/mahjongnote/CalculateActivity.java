package com.example.mahjongnote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CalculateActivity extends AppCompatActivity {

    private static final List<Button> fuButtons = new ArrayList<>();

    private static final List<Fan> FANS = Arrays.asList(
            new Fan(R.string.lizhi, 1, 0),
            new Fan(R.string.yifa, 1, 0),
            new Fan(R.string.menqingzimo, 1, 0),
            new Fan(R.string.duanyaojiu, 1, 1),
            new Fan(R.string.pinghu, 1, 0),
            new Fan(R.string.yibeikou, 1, 0),
            new Fan(R.string.yipai, 1, 1),
            new Fan(R.string.lingshangkaihua, 1, 1),
            new Fan(R.string.qianggang, 1, 1),
            new Fan(R.string.haidilaoyue, 1, 1),
            new Fan(R.string.hedilaoyu, 1, 1),

            new Fan(R.string.sansetongshun, 2, 1),
            new Fan(R.string.sansetongke, 2, 2),
            new Fan(R.string.yiqitongguan, 2, 1),
            new Fan(R.string.duiduihu, 2, 2),
            new Fan(R.string.sananke, 2, 2),
            new Fan(R.string.qiduizi, 2, 0),
            new Fan(R.string.hunquandaiyaojiu, 2, 1),
            new Fan(R.string.hunlaotou, 2, 2),
            new Fan(R.string.sangangzi, 2, 2),
            new Fan(R.string.shuanglizhi, 2, 0),

            new Fan(R.string.hunyise, 3, 2),
            new Fan(R.string.chunquandaiyaojiu, 3, 2),
            new Fan(R.string.erbeikou, 3, 0),

            new Fan(R.string.qingyise, 6, 5),

            new Fan(R.string.yimanbeizhu, 13, 13)
    );

    private static final int[] FAN_LABEL_RESOURCE_ID = {
            R.id.yifan,
            R.id.liangfan,
            R.id.sanfan,
            R.id.liufan,
            R.id.yiman
    };

    private static final int[] FU_BUTTON_RESOURCE_ID = {
            R.id.fu20,
            R.id.fu30,
            R.id.fu40,
            R.id.fu50,
            R.id.fu60,
            R.id.fu70,
            R.id.fu80,
            R.id.fu90,
            R.id.fu100,
            R.id.fu110
    };
    private static final int DEFAULT_CLICKED_FU_BUTTON_RESOURCE_ID = R.id.fu30;

    private Button clickedFuButton = null;

    private static boolean zimo;
    private static boolean zhuangWin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate);

        // Add all the checkboxes
        ViewGroup layout = null;
        int previousFan = 0;
        for(int i = 0; i < FANS.size(); ++i) {
            Fan fan = FANS.get(i);
            if (fan.getFanshu() != previousFan) {
                // update layout
                int index = (int) Math.floor(Math.log(fan.getFanshu()) * 1.9);
                layout = (ViewGroup) findViewById(FAN_LABEL_RESOURCE_ID[index]);
                // update previousFan
                previousFan = fan.getFanshu();
            }

            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(fan.getName());
            checkBox.setId(i);

            layout.addView(checkBox, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // Find all the buttons for Fu (20~110)
        fuButtons.clear();
        for (int fuButtonResourceId : FU_BUTTON_RESOURCE_ID) {
            fuButtons.add((Button) findViewById(fuButtonResourceId));
        }
        clickedFuButton = (Button) findViewById(DEFAULT_CLICKED_FU_BUTTON_RESOURCE_ID);
        clickedFuButton.setTextColor(Color.BLUE);

        Intent intent = getIntent();
        zimo = intent.getBooleanExtra("zimo", false);
        zhuangWin = intent.getBooleanExtra("zhuang_win", false);
        // When dian, 20Fu is not possible
        if (zimo) {
            ((Button) findViewById(R.id.fu20)).setEnabled(true);
        } else {
            ((Button) findViewById(R.id.fu20)).setEnabled(false);
        }
    }

    /**
     * onClick event for Fu buttons
     */
    public void fuButtonClicked(View view) {
        clickedFuButton.setTextColor(Color.BLACK);
        ((Button) view).setTextColor(Color.BLUE);
        clickedFuButton = (Button) view;
    }

    /**
     * onClick event for the Confirm button
     */
    public void confirm(View view) {
        // Dora
        int dora = Integer.parseInt(
                ((EditText) findViewById(R.id.editTextDora)).getText().toString());

        // Fan
        List<Fan> clickedFans = new ArrayList<>();
        for (int i = 0; i < FANS.size(); ++i) {
            if (((CheckBox) findViewById(i)).isChecked()) {
                clickedFans.add(FANS.get(i));
            }
        }
        clickedFans.add(new Fan(R.string.dora, dora, dora));

        // Others
        int fu = (fuButtons.indexOf(clickedFuButton) + 2) * 10;
        boolean fulu = ((CheckBox) findViewById(R.id.fulu)).isChecked();

        int totalScore;
        try {
            if (zhuangWin) {
                totalScore = ScoreCalculator.calculateTotalScoreZhuang(clickedFans, fu, fulu);
            } else {
                totalScore = ScoreCalculator.calculateTotalScoreXian(clickedFans, fu, fulu);
            }
        } catch (IllegalArgumentException e) {
            String conflictFan = getResources().getString(Integer.parseInt(e.getMessage()));
            new AlertDialog.Builder(this)
                    .setTitle("Invalid input")
                    .setMessage(conflictFan + " is not possible here.")
                    .setPositiveButton(android.R.string.yes, null)
                    .show();
            return;
        }

        String csvString = "";
        for (Fan fan : clickedFans) {
            csvString += "," + fan.getName();
        }

        // Send result back
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", totalScore);
        returnIntent.putExtra("csv", csvString);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
}
