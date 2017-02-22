package com.example.mahjongnote;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.List;

final class GameStatus {

    private static final List<String> CHANGS = Arrays.asList(
            MainActivity.getContext().getString(R.string.east),
            MainActivity.getContext().getString(R.string.south),
            MainActivity.getContext().getString(R.string.west),
            MainActivity.getContext().getString(R.string.north)
    );
    private static final List<String> NUMBERS = Arrays.asList(
            MainActivity.getContext().getString(R.string.one),
            MainActivity.getContext().getString(R.string.two),
            MainActivity.getContext().getString(R.string.three),
            MainActivity.getContext().getString(R.string.four),
            MainActivity.getContext().getString(R.string.five),
            MainActivity.getContext().getString(R.string.six),
            MainActivity.getContext().getString(R.string.seven),
            MainActivity.getContext().getString(R.string.eight),
            MainActivity.getContext().getString(R.string.nine),
            MainActivity.getContext().getString(R.string.ten)
    );

    private int zhuangIndex = 0;
    private int changName = 0; // 0:东 4:南 8:西 12:北
    private int changNum = 0; // 几本场
    private int lizhiStickNum = 0;

    private int currentPlayer = 0;
    private List<Integer> scores = Arrays.asList(25000, 25000, 25000, 25000);

    public String getChangFullName() {
        return CHANGS.get(changName / 4) + NUMBERS.get(changName % 4)
                + MainActivity.getContext().getString(R.string.ju)
                + (changNum == 0 ? "" : NUMBERS.get(changNum - 1)
                        + MainActivity.getContext().getString(R.string.benchang));
    }

    public String getCsvString() {
        return getChangFullName() + "," + TextUtils.join(",", scores);
    }

    public void increaseLizhiStickNum() {
        ++lizhiStickNum;
    }

    public void increaseChangName() {
        ++changName;
    }

    public void increaseChangNum() {
        ++changNum;
    }

    public void increaseZhuangIndex() {
        ++zhuangIndex;
        if (zhuangIndex == 4) {
            zhuangIndex = 0;
        }
    }

    public void updateScore(int index, int delta) {
        scores.set(index, scores.get(index) + delta);
    }

    @Override
    public String toString() {
        return "changName:" + changName
                + " changNum:" + changNum
                + " zhuang:" + zhuangIndex
                + " lizhiStick:" + lizhiStickNum
                + " scores:" + TextUtils.join(", ", scores);
    }

    // Generated getter and setter

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public int getZhuangIndex() {
        return zhuangIndex;
    }

    public void setZhuangIndex(int zhuangIndex) {
        this.zhuangIndex = zhuangIndex;
    }

    public int getChangNum() {
        return changNum;
    }

    public void setChangNum(int changNum) {
        this.changNum = changNum;
    }

    public int getLizhiStickNum() {
        return lizhiStickNum;
    }

    public void setLizhiStickNum(int lizhiStickNum) {
        this.lizhiStickNum = lizhiStickNum;
    }

    public List<Integer> getScores() {
        return scores;
    }
}
