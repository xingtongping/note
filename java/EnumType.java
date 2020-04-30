/**
 * 枚举测试
 */
public enum  EnumType {

    VIDEO(1, "视频"), AUDIO(2, "音频"), TEXT(3, "文本"), IMAGE(4, "图像");

    int value;
    String name;

    EnumType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }


}
