package my.reqqpe.rseller.utils;

import lombok.Setter;

import java.util.logging.Logger;

public class LoggerUtil {

    @Setter
    private static Logger logger;

    public static void warn(String message) {
        if (message == null || message.isEmpty()) {
            message = "not found message";
        }
        logger.warning(message);
    }
    public static void info(String message) {
        if (message == null || message.isEmpty()) {
            message = "not found message";
        }
        logger.info(message);
    }
    public static void fine(String message) {
        if (message == null || message.isEmpty()) {
            message = "not found message";
        }
        logger.fine(message);
    }
}
