package com.the_coffe_coders.fastestlap.domain.grand_prix;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Practice extends Session {
    private int number;

    public Practice(String date, String time, int number) {
        super(date, time);
        this.number = number;
        setEndDateTime();
    }

    public String getPractice() {
        return "Practice" + number;
    }
}
