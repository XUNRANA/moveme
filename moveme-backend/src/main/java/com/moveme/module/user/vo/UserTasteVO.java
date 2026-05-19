package com.moveme.module.user.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UserTasteVO {
    private BigDecimal avgRatingGiven;
    private long ratingCount;
    private List<GenrePref> genrePrefs;
    private List<PersonPref> personPrefs;

    @Data
    public static class GenrePref {
        private Integer genreId;
        private String genreName;
        private BigDecimal score;
    }

    @Data
    public static class PersonPref {
        private Long personId;
        private String personName;
        private String roleKind;
        private BigDecimal score;
    }
}
