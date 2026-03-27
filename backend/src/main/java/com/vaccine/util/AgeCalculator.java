package com.vaccine.util;

import java.time.LocalDate;
import java.time.Period;

public final class AgeCalculator {
    private AgeCalculator() {
    }

    public static Integer calculateAge(LocalDate dob) {
        if (dob == null) {
            return null;
        }
        return Math.max(Period.between(dob, LocalDate.now()).getYears(), 0);
    }
}
