package com.the_coffe_coders.fastestlap.dto;

public class ConstructorStandingsDTO {

    private String position;
    private String positionText;
    private String points;
    private String wins;
    private ConstructorDTO constructor;

    public ConstructorStandingsDTO(String position, String positionText, String points, String wins, ConstructorDTO constructor) {
        this.position = position;
        this.positionText = positionText;
        this.wins = wins;
        this.constructor = constructor;
    }

    public String getPosition(){
        return this.position;
    }

    public String getPositionText(){
        return this.positionText;
    }

    public String getPoints(){
        return this.points;
    }

    public String getWins(){
        return this.wins;
    }

   public ConstructorDTO getConstructor(){
       return this.constructor;
   }




}
