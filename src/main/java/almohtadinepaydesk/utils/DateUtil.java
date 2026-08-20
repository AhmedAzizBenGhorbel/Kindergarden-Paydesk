package almohtadinepaydesk.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Small date-format helper used by the UI.
 * It keeps date formatting consistent across tables and detail screens.
 */
public class DateUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Formats a LocalDate in the French day/month/year style used by the app.
     *
     * @param date date to format
     * @return the formatted date, or an empty string when the value is null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }

        return date.format(DATE_FORMATTER);
    }

    /**
     * Utility class, so it should not be instantiated.
     */
    private DateUtil() {
    }
}
