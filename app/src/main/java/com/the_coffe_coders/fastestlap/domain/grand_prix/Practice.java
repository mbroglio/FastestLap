package com.the_coffe_coders.fastestlap.domain.grand_prix;

public class Practice extends Session {
    private int number;

    public Practice(String date, String time, int number) {
        super(date, time);
        this.number = number;
        setEndDateTime();
    }

    public Practice() {
        super();
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getPractice() {
        return "Practice" + number;
    }
}
