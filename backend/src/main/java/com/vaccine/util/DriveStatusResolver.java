package com.vaccine.util;

import com.vaccine.domain.VaccinationDrive;

import java.time.LocalDateTime;

public final class DriveStatusResolver {

    private DriveStatusResolver() {
    }

    public static String resolve(VaccinationDrive drive) {
        return resolve(drive, LocalDateTime.now());
    }

    public static String resolve(VaccinationDrive drive, LocalDateTime now) {
        LocalDateTime start = resolveStart(drive);
        LocalDateTime end = resolveEnd(drive);

        if (start == null || end == null) {
            return "EXPIRED";
        }
        if (now.isBefore(start)) {
            return "UPCOMING";
        }
        if (now.isAfter(end)) {
            return "EXPIRED";
        }
        return "LIVE";
    }

    public static LocalDateTime resolveStart(VaccinationDrive drive) {
        if (drive == null || drive.getDriveDate() == null || drive.getStartTime() == null) {
            return null;
        }
        return drive.getDriveDate().atTime(drive.getStartTime());
    }

    public static LocalDateTime resolveEnd(VaccinationDrive drive) {
        if (drive == null || drive.getDriveDate() == null || drive.getEndTime() == null) {
            return null;
        }

        LocalDateTime start = resolveStart(drive);
        LocalDateTime end = drive.getDriveDate().atTime(drive.getEndTime());
        if (start != null && end.isBefore(start)) {
            return end.plusDays(1);
        }
        return end;
    }
}
