/*
 * Copyright (c) 2021. Nils Witt
 */

package de.nilswitt.splan;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    /**
     * List of (German) months
     */
    private final List<String> months = new ArrayList<>();

    public Utils() {
        months.add("Januar");
        months.add("Februar");
        months.add("März");
        months.add("April");
        months.add("Mai");
        months.add("Juni");
        months.add("Juli");
        months.add("August");
        months.add("September");
        months.add("Oktober");
        months.add("November");
        months.add("Dezember");
    }

    public String convertDate(String oldDate) {
        String[] parts = oldDate.split(" ");
        int monthInt = months.indexOf(parts[2]) + 1;
        String year = parts[3];
        String month = Integer.toString(monthInt);
        String day = parts[1].substring(0, parts[1].length() - 1);
        if (day.length() == 1) {
            day = 0 + day;
        }
        return year + "-" + month + "-" + day;
    }
}
