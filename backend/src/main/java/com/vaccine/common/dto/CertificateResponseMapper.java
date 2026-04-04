package com.vaccine.common.dto;

import com.vaccine.domain.Certificate;
import com.vaccine.domain.Slot;
import com.vaccine.domain.User;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class CertificateResponseMapper {
    private final UserRepository userRepository;

    public CertificateResponseMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CertificateResponse toResponse(Certificate cert) {
        var booking = cert.getBooking();
        User user = booking != null ? booking.getUser() : null;
        Slot slot = booking != null ? booking.getSlot() : null;
        VaccinationDrive drive = slot != null ? slot.getDrive() : null;
        VaccinationCenter center = drive != null ? drive.getCenter() : null;
        String location = buildLocation(
            center != null ? center.getAddress() : null,
            center != null ? center.getCity() : null,
            center != null ? center.getState() : null,
            center != null ? center.getPincode() : null
        );
        String verifiedBy = resolveVerifiedBy(booking != null ? booking.getAdminId() : null);
        String verificationUrl = cert.getId() != null ? "/verify-certificate?certId=" + cert.getId() : null;

        return new CertificateResponse(
            cert.getId(),
            booking != null ? booking.getId() : null,
            cert.getCertificateNumber(),
            cert.getVaccineName(),
            cert.getDoseNumber(),
            resolveDoseLabel(cert),
            cert.getVaccinationDate(),
            cert.getNextDoseDate(),
            cert.getQrCode(),
            cert.getQrCodeData(),
            verificationUrl,
            cert.getIssuedAt(),
            user != null ? user.getId() : null,
            user != null && user.getId() != null ? String.valueOf(user.getId()) : null,
            user != null ? user.getFullName() : null,
            user != null ? user.getFullName() : null,
            null,
            user != null ? user.getDob() : null,
            user != null ? user.getEmail() : null,
            center != null ? center.getName() : null,
            center != null ? center.getAddress() : null,
            location,
            drive != null ? drive.getTitle() : null,
            slot != null ? slot.getDateTime() : null,
            slot != null && slot.getStartTime() != null && slot.getEndTime() != null
                ? slot.getStartTime() + " - " + slot.getEndTime()
                : null,
            cert.getDigitalVerificationCode(),
            verifiedBy
        );
    }

    private String resolveDoseLabel(Certificate cert) {
        Integer doseNumber = cert.getDoseNumber();
        if (doseNumber == null) {
            return "N/A";
        }

        if (doseNumber == 3) {
            return "Booster Dose";
        }
        if (doseNumber == 1 && cert.getNextDoseDate() != null) {
            return "Dose 1 of 2";
        }
        if (doseNumber == 2) {
            return "Dose 2 of 2";
        }
        return "Dose " + doseNumber;
    }

    private String resolveVerifiedBy(Long adminId) {
        if (adminId == null) {
            return "VaxZone System";
        }
        return userRepository.findById(adminId)
            .map(User::getFullName)
            .filter(name -> !name.isBlank())
            .orElse("VaxZone System");
    }

    private String buildLocation(String address, String city, String state, String pincode) {
        return java.util.stream.Stream.of(address, city, state, pincode)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .collect(Collectors.joining(", "));
    }
}
