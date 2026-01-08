package tv.tvai.like.enums;

public enum OptionKeyEnum {
    ATTR("attr", true),
    LIMIT("limit", true),
    IMG_RATIO("img-ratio");

    private final String key;
    private final boolean executable; //可执行的 代表需要处理

    OptionKeyEnum(String key) {
        this.key = key;
        this.executable = false;
    }

    OptionKeyEnum(String key, boolean executable) {
        this.key = key;
        this.executable = executable;
    }

    public String getKey() {
        return key;
    }

    public boolean isExecutable() {
        return executable;
    }

    public static boolean executable(String key) {
        for (OptionKeyEnum value : values()) {
            if (value.key.equals(key)) {
                return value.executable;
            }
        }
        return false;
    }
}
