package com.example.mahjongnote;

import java.util.List;

final class ScoreCalculator {

    private static final int[][] zhuangSmallTable = {
            {0, 1500, 2000, 2400, 2900, 3400, 3900, 4400, 4800, 5300},
            {2100, 2900, 3900, 4800, 5800, 6800, 7700, 8700, 9600, 10600},
            {3900, 5800, 7700, 9600, 11600, 12000, 12000, 12000, 12000, 12000},
            {7800, 11600, 12000, 12000, 12000, 12000, 12000, 12000, 12000, 12000}
    };
    private static final int[] zhuangLargeTable = {
            12000, 18000, 18000, 24000, 24000, 24000, 36000, 36000, 48000
    };
    private static final int[] zhuangQiduiziTable = {
            2400, 4800, 9600, 12000, 18000, 18000, 24000, 24000, 24000, 36000, 36000, 48000
    };
    private static final int[][] xianSmallTable = {
            {0, 1000, 1300, 1600, 2000, 2300, 2600, 2900, 3200, 3600},
            {1400, 2000, 2600, 3200, 3900, 4500, 5200, 5800, 6400, 7100},
            {2600, 3900, 5200, 6400, 7000, 8000, 8000, 8000, 8000, 8000},
            {5200, 7700, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000}
    };
    private static final int[] xianLargeTable = {
            8000, 12000, 12000, 16000, 16000, 16000, 24000, 24000, 32000
    };
    private static final int[] xianQiduiziTable = {
            1600, 3200, 6400, 8000, 12000, 12000, 16000, 16000, 16000, 24000, 24000, 32000
    };

    static int calculateTotalScoreZhuang(List<Fan> fans, int fu, boolean fulu) {
        int fanSum = calculateFanSum(fans, fu, fulu);
        if (fanSum < 0) {
            return zhuangQiduiziTable[-fanSum - 2];
        }
        if (fanSum < 5) {
            return zhuangSmallTable[fanSum - 1][fu / 10 - 2];
        }
        return zhuangLargeTable[fanSum - 5];
    }

    static int calculateTotalScoreXian(List<Fan> fans, int fu, boolean fulu) {
        int fanSum = calculateFanSum(fans, fu, fulu);
        if (fanSum < 0) {
            return xianQiduiziTable[-fanSum - 2];
        }
        if (fanSum < 5) {
            return xianSmallTable[fanSum - 1][fu / 10 - 2];
        }
        return xianLargeTable[fanSum - 5];
    }

    static int div2(int score) {
        return ((int) Math.ceil(score / 200.0)) * 100;
    }
    static int div3(int score) {
        return ((int) Math.ceil(score / 300.0)) * 100;
    }

    /**
     * Get the sum of fanshu, return -fanshu when Qiduizi is included
     */
    private static int calculateFanSum(List<Fan> fans, int fu, boolean fulu) {
        int fanSum = 0;
        boolean qiduizi = false;
        for (Fan fan : fans) {
            // Check qiduizi
            if (fan.getNameResourceId() == R.string.qiduizi) {
                qiduizi = true;
            }

            if (!fulu) {
                fanSum += fan.getFanshu();
            } else {
                if (fan.getFeimengqingFanshu() == 0) {
                    throw new IllegalArgumentException(fan.getNameResourceId() + "");
                }
                fanSum += fan.getFeimengqingFanshu();
            }
        }

        if (qiduizi) {
            return Math.max(-fanSum, -13);
        }
        return Math.min(fanSum, 13);
    }
}
