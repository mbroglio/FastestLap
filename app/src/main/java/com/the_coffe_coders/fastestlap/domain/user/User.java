package com.the_coffe_coders.fastestlap.domain.user;
import com.google.firebase.database.Exclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class User {
    private String name;
    private String email;
    private String idToken;

    @Exclude
    public String getIdToken() {
        return idToken;
    }
}
