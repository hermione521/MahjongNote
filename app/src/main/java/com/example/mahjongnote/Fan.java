package com.example.mahjongnote;

final class Fan {
    public int getNameResourceId() {
        return nameResourceId;
    }

    public int getFanshu() {
        return fanshu;
    }

    public int getFeimengqingFanshu() {
        return feimengqingFanshu;
    }

    private int nameResourceId;
    private int fanshu;
    private int feimengqingFanshu;

    public Fan(int nameResourceId, int fanshu, int feimengqingFanshu) {
        this.nameResourceId = nameResourceId;
        this.fanshu = fanshu;
        this.feimengqingFanshu = feimengqingFanshu;
    }
}
