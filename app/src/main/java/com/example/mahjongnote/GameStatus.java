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

    private List<Integer> scores = Arrays.asList(25000, 25000, 25000, 25000);

    public GameStatus() {}
    public GameStatus(String lastRecord, List<String> usernames, List<Integer> scores) {
        setGameFromLastRecord(lastRecord, usernames);
        this.scores = scores;

        int scoreLeft = 100000;
        for (int score : scores) {
            scoreLeft -= score;
        }
        lizhiStickNum = scoreLeft / 1000;
    }

    private void setGameFromLastRecord(String lastRecord, List<String> usernames) {
        // Set chang with last record
        setChangWithChangFullName(lastRecord.substring(0, lastRecord.indexOf(",")));

        String[] split = lastRecord.split(",");
        // Case 1: lizhi
        // The last game was not finished, use the current chang
        if (split[0].equals(MainActivity.getContext().getString(R.string.lizhi))) {
            return;
        }

        // Case 2: zimo
        // Case 3: dian
        // Check if the winner is zhuang
        if (split[0].equals(MainActivity.getContext().getString(R.string.zimo)) ||
                split[0].equals(MainActivity.getContext().getString(R.string.dian))) {
            if (split[2].equals(usernames.get(zhuangIndex))) {
                // The winner is zhuang
                increaseChangNum();
            } else {
                // Guozhuang
                increaseChangName();
                changNum = 0;
            }
            return;
        }

        // Case 4: liuju
        // Check if zhuang tingpai
        if (split[0].equals(MainActivity.getContext().getString(R.string.liuju))) {
            boolean zhuangTingpai = false;
            for (int i = 2; i < split.length; ++i) {
                if (split[i].equals(usernames.get(zhuangIndex))) {
                    zhuangTingpai = true;
                    break;
                }
            }
            if (!zhuangTingpai) {
                increaseChangName();
            }
            increaseChangNum();
            return;
        }

        // Case default: should not happen
    }

    private void setChangWithChangFullName(String changFullName) {
        // Set changName
        for (int i = 0; i < 4; ++i) {
            if (CHANGS.get(i).equals(changFullName.substring(0, 1))) {
                changName = i * 4;
                break;
            }
        }
        for (int i = 0; i < 4; ++i) {
            if (NUMBERS.get(i).equals(changFullName.substring(1, 2))) {
                changName += i;
                zhuangIndex = i;
                break;
            }
        }

        // Set changNum
        if (changFullName.length() == 3) {
            return;
        }
        for (int i = 0; i < 10; ++i) {
            if (NUMBERS.get(i).equals(changFullName.substring(3, 4))) {
                changNum = i + 1;
                break;
            }
        }
    }

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
    public int getZhuangIndex() {
        return zhuangIndex;
    }

    public void setZhuangIndex(int zhuangIndex) {
        this.zhuangIndex = zhuangIndex;
    }

    public int getChangName() {
        return changName;
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
