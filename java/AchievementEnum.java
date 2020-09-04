package com.mg.tb.enums;

public enum AchievementEnum {

    ACHIEVEMENT1(1L, "夏日戀曲","name_en"),
    ACHIEVEMENT2(2L, "冬季戀歌","name_en"),
    ACHIEVEMENT3(3L, "亞洲之龍","name_en"),
    ACHIEVEMENT4(4L, "歐洲旅王","name_en"),
    ACHIEVEMENT5(5L, "非洲獨秀","name_en"),
    ACHIEVEMENT6(6L, "大洋洲之冠","name_en"),
    ACHIEVEMENT7(7L, "南美洲之選","name_en"),
    ACHIEVEMENT8(8L, "專攻北美洲","name_en"),
    ACHIEVEMENT9(9L, "旅人的請託","travlerWish"),
    ACHIEVEMENT10(10L, "韓國旅王走透透","name_en"),
    ACHIEVEMENT11(11L, "TripBay職人","name_en"),
    ACHIEVEMENT12(12L, "去唄人氣王","popularKing"),
    ACHIEVEMENT13(13L, "去唄銷售王","name_en"),
    ACHIEVEMENT14(14L, "去唄百分百","name_en"),
    ACHIEVEMENT15(15L, "TripBay初心者","name_en"),
    ACHIEVEMENT16(16L, "五星賣家推薦","name_en"),
    ACHIEVEMENT17(17L, "94愛購物","name_en"),
    ACHIEVEMENT18(18L, "旅人雜貨店","name_en"),
    ACHIEVEMENT19(19L, "日本旅王走透透","name_en"),
    ACHIEVEMENT20(20L, "美國旅王走透透","name_en"),
    ACHIEVEMENT21(21L, "TOP旅人","name_en"),
    ACHIEVEMENT22(22L, "Trip任務王","name_en"),
    ACHIEVEMENT23(23L, "零負評達人激賞","name_en"),
    ACHIEVEMENT24(24L, "雙11採購狂歡","name_en"),
    ACHIEVEMENT25(25L, "TripBay 內行人","name_en"),
    ACHIEVEMENT26(26L, "一步一腳印","name_en"),
    ACHIEVEMENT27(27L, "心願便利貼","name_en"),
    ACHIEVEMENT28(28L, "94 愛sharing","name_en"),
    ACHIEVEMENT29(29L, "Trip新人王","name_en"),
    ACHIEVEMENT30(30L, "Trip開幕禮","name_en"),
    NULL(0L,"null","null");

    Long value;
    String name;
    String name_en;

    AchievementEnum(Long value, String name,String name_en) {
        this.value = value;
        this.name = name;
        this.name_en = name_en;
    }

    public Long getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getNameEn() {
        return name_en;
    }

    /*
     * 匹配操作码
     * */
    public static AchievementEnum matchOpCode(Long opCodeStr) {
        for (AchievementEnum opCode : AchievementEnum.values()) {
            if (opCode.value == opCodeStr) {
                return opCode;
            }
        }
        return AchievementEnum.NULL;
    }

}
