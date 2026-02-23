package my.reqqpe.rseller.utils;

import lombok.Setter;

import java.util.logging.Logger;

public class LoggerUtil {

    @Setter
    private static Logger logger;

    public static void warn(String message) {
        logger.warning(message);
    }
    public static void info(String message) {
        logger.info(message);
    }
    public static void fine(String message) {
        logger.fine(message);
    }
}
