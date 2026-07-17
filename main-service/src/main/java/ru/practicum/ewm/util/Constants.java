package ru.practicum.ewm.util;

import java.time.format.DateTimeFormatter;

public final class Constants {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final String APP_NAME = "ewm-main-service";

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private Constants() {
    }
}
