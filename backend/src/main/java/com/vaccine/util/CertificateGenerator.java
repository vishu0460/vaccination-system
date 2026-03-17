package com.vaccine.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vaccine.domain.Certificate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
public class CertificateGenerator {

    /**
     * Generate QR code as PNG byte array
     */
    public byte[] generateQrCodePng(String data, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Generate QR code as Base64 string (for embedding in HTML)
     */
    public String generateQrCodeBase64(String data, int width, int height) throws WriterException, IOException {
        byte[] pngBytes = generateQrCodePng(data, width, height);
        return Base64.getEncoder().encodeToString(pngBytes);
    }

    /**
     * Save QR code to file
     */
    public String saveQrCodeToFile(String data, String filePath, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
        
        Path path = java.nio.file.Paths.get(filePath);
        Files.createDirectories(path.getParent());
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        return filePath;
    }

    /**
     * Get certificate details for display
     */
    public CertificateDetails getCertificateDetails(Certificate certificate) {
        return new CertificateDetails(
            certificate.getCertificateNumber(),
            certificate.getBooking().getUser().getFullName(),
            certificate.getBooking().getUser().getEmail(),
            certificate.getVaccineName(),
            certificate.getDoseNumber(),
            certificate.getBooking().getSlot().getDrive().getCenter().getName(),
            certificate.getBooking().getSlot().getDrive().getTitle(),
            certificate.getBooking().getSlot().getStartTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
            certificate.getIssuedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
            certificate.getNextDoseDate() != null 
                ? certificate.getNextDoseDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                : "N/A"
        );
    }

    /**
     * Certificate details DTO
     */
    public record CertificateDetails(
        String certificateNumber,
        String beneficiaryName,
        String email,
        String vaccineName,
        Integer doseNumber,
        String centerName,
        String driveTitle,
        String vaccinationDate,
        String issuedDate,
        String nextDoseDate
    ) {}
}
