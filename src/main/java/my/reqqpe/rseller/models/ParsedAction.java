package my.reqqpe.rseller.models;

import my.reqqpe.rseller.utils.Parse;

import java.util.ArrayList;
import java.util.List;

public record ParsedAction(String action, String data) {


    public static ParsedAction parse(String actionLine) {
        String cmd = actionLine.trim();

        int start = cmd.indexOf('[');
        int end = cmd.indexOf(']');

        if (start != 0 || end == -1) {
            return new ParsedAction("text", cmd); // fallback
        }

        String action = cmd.substring(start + 1, end).trim().toLowerCase();
        String data = cmd.substring(end + 1).trim();

        return new ParsedAction(action, data);
    }
    public static List<ParsedAction> parse(List<String> actions) {
        List<ParsedAction> result = new ArrayList<>();
        for (String action : actions) {
            ParsedAction pa = parse(action);
            result.add(pa);
        }
        return result;
    }
}
