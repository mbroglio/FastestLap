package com.the_coffe_coders.fastestlap.domain.nation;

public class Nation {
    private String nationId;
    private String abbreviation;
    private String nation_flag_url;

    public Nation(String nationId, String abbreviation, String nation_flag_url) {
        this.nationId = nationId;
        this.abbreviation = abbreviation;
        this.nation_flag_url = nation_flag_url;
    }

    public Nation() {
    }

    public String getNationId() {
        return nationId;
    }

    public void setNationId(String nationId) {
        this.nationId = nationId;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getNation_flag_url() {
        return nation_flag_url;
    }

    public void setNation_flag_url(String nation_flag_url) {
        this.nation_flag_url = nation_flag_url;
    }

    @Override
    public String toString() {
        return "Nation{" +
                "nationId='" + nationId + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                ", nation_flag_url='" + nation_flag_url + '\'' +
                '}';
    }
}
