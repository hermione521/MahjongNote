package com.example.mahjongnote;

final class Fan {

    private int nameResourceId;
    private int fanshu;
    private int feimengqingFanshu;

    public Fan(int nameResourceId, int fanshu, int feimengqingFanshu) {
        this.nameResourceId = nameResourceId;
        this.fanshu = fanshu;
        this.feimengqingFanshu = feimengqingFanshu;
    }

    public String getName() {
        String name = MainActivity.getContext().getString(nameResourceId);
        if (nameResourceId == R.string.dora || nameResourceId == R.string.yipai) {
            name += fanshu;
        }
        return name;
    }

    public int getNameResourceId() {
        return nameResourceId;
    }

    public int getFanshu() {
        return fanshu;
    }

    public int getFeimengqingFanshu() {
        return feimengqingFanshu;
    }
}
