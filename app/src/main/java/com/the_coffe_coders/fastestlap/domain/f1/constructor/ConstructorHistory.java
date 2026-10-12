package com.the_coffe_coders.fastestlap.domain.f1.constructor;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ConstructorHistory implements Serializable {
    private String podiums;
    private String points;
    private String position;
    private String wins;
    private String year;
}
