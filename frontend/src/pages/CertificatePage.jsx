import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/client';
import Skeleton from '../components/Skeleton';
import EmptyState from '../components/EmptyState';
import { jsPDF } from 'jspdf';
import QRCode from 'qrcode';

export default function CertificatePage() {
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedCert, setSelectedCert] = useState(null);
  const [qrCodeUrl, setQrCodeUrl] = useState(null);
  const [showQrModal, setShowQrModal] = useState(false);

  useEffect(() => {
    fetchCertificates();
  }, []);

  const fetchCertificates = async () => {
    try {
      const response = await apiClient.get('/certificates/my-certificates');
      setCertificates(response.data);
    } catch (err) {
      console.error('Failed to fetch certificates');
    } finally {
      setLoading(false);
    }
  };

  // Generate QR code data URL from verification code
  const generateQRCode = async (cert) => {
    try {
      const verificationUrl = `${window.location.origin}/verify/certificate/${cert.certificateNumber}`;
      const qrDataUrl = await QRCode.toDataURL(verificationUrl, {
        width: 200,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#ffffff'
        }
      });
      return qrDataUrl;
    } catch (err) {
      console.error('Failed to generate QR code:', err);
      return null;
    }
  };

  // Handle QR code scan - show modal with certificate details
  const handleShowQrCode = async (cert) => {
    setSelectedCert(cert);
    const qrUrl = await generateQRCode(cert);
    setQrCodeUrl(qrUrl);
    setShowQrModal(true);
  };

  const generatePNG = (cert) => {
    // Create a canvas-based PNG certificate
    const canvas = document.createElement('canvas');
    canvas.width = 800;
    canvas.height = 600;
    const ctx = canvas.getContext('2d');

    // Background
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Header background
    ctx.fillStyle = '#198754';
    ctx.fillRect(0, 0, canvas.width, 100);

    // Title
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 36px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('VACCINATION CERTIFICATE', canvas.width / 2, 60);

    // Certificate number
    ctx.fillStyle = '#333333';
    ctx.font = '20px Arial';
    ctx.fillText(`Certificate No: ${cert.certificateNumber || 'N/A'}`, canvas.width / 2, 130);

    // Divider line
    ctx.strokeStyle = '#198754';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(50, 150);
    ctx.lineTo(750, 150);
    ctx.stroke();

    // Certificate content
    ctx.font = '18px Arial';
    ctx.textAlign = 'left';
    
    const content = [
      { label: 'Beneficiary Name:', value: cert.userName || 'N/A' },
      { label: 'Email:', value: cert.userEmail || 'N/A' },
      { label: 'Vaccine Name:', value: cert.vaccineName || 'N/A' },
      { label: 'Dose Number:', value: cert.doseNumber ? `Dose ${cert.doseNumber}` : 'N/A' },
      { label: 'Vaccination Center:', value: cert.centerName || 'N/A' },
      { label: 'Drive:', value: cert.driveTitle || 'N/A' },
      { label: 'Date of Vaccination:', value: cert.slotDateTime ? new Date(cert.slotDateTime).toLocaleDateString() : 'N/A' },
      { label: 'Date of Issue:', value: cert.issuedAt ? new Date(cert.issuedAt).toLocaleDateString() : 'N/A' },
    ];

    if (cert.nextDoseDate) {
      content.push({ label: 'Next Dose Date:', value: new Date(cert.nextDoseDate).toLocaleDateString() });
    }

    content.push({ label: 'Digital Verification Code:', value: cert.digitalVerificationCode || 'N/A' });

    let y = 180;
    content.forEach(item => {
      ctx.fillStyle = '#666666';
      ctx.fillText(item.label, 80, y);
      ctx.fillStyle = '#333333';
      ctx.font = 'bold 18px Arial';
      ctx.fillText(item.value, 320, y);
      ctx.font = '18px Arial';
      y += 35;
    });

    // QR Code placeholder
    if (cert.qrCode) {
      ctx.fillStyle = '#999999';
      ctx.font = '12px Arial';
      ctx.textAlign = 'center';
      ctx.fillText('Scan QR Code to Verify', canvas.width / 2, 520);
    }

    // Footer
    ctx.fillStyle = '#198754';
    ctx.fillRect(0, 550, canvas.width, 50);
    ctx.fillStyle = '#ffffff';
    ctx.font = '14px Arial';
    ctx.fillText('This is a government-issued vaccination certificate', canvas.width / 2, 575);

    // Download as PNG
    const link = document.createElement('a');
    link.download = `certificate-${cert.certificateNumber}.png`;
    link.href = canvas.toDataURL('image/png');
    link.click();
  };

  const generatePDF = async (cert) => {
    try {
      const doc = new jsPDF();
      
      // Header
      doc.setFillColor(25, 135, 84);
      doc.rect(0, 0, 210, 40, 'F');
      
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(24);
      doc.setFont('helvetica', 'bold');
      doc.text('VACCINATION CERTIFICATE', 105, 25, { align: 'center' });
      
      // Certificate Number
      doc.setTextColor(51, 51, 51);
      doc.setFontSize(12);
      doc.setFont('helvetica', 'normal');
      doc.text(`Certificate No: ${cert.certificateNumber || 'N/A'}`, 105, 55, { align: 'center' });
      
      // Divider line
      doc.setDrawColor(25, 135, 84);
      doc.setLineWidth(1);
      doc.line(20, 60, 190, 60);
      
      // Content
      doc.setFontSize(11);
      let y = 75;
      
      const addField = (label, value, isBold = false) => {
        doc.setFont('helvetica', isBold ? 'bold' : 'normal');
        doc.setTextColor(102, 102, 102);
        doc.text(label, 25, y);
        doc.setTextColor(51, 51, 51);
        doc.text(String(value || 'N/A'), 80, y);
        y += 10;
      };
      
      addField('Beneficiary Name:', cert.userName || 'N/A', true);
      addField('Email:', cert.userEmail || 'N/A');
      addField('Vaccine Name:', cert.vaccineName || 'N/A', true);
      addField('Dose Number:', cert.doseNumber ? `Dose ${cert.doseNumber}` : 'N/A');
      addField('Vaccination Center:', cert.centerName || 'N/A', true);
      addField('Drive:', cert.driveTitle || 'N/A');
      addField('Vaccination Date:', cert.slotDateTime ? new Date(cert.slotDateTime).toLocaleDateString() : 'N/A', true);
      addField('Date of Issue:', cert.issuedAt ? new Date(cert.issuedAt).toLocaleDateString() : 'N/A');
      
      if (cert.nextDoseDate) {
        addField('Next Dose Date:', new Date(cert.nextDoseDate).toLocaleDateString(), true);
      }
      
      y += 5;
      doc.line(20, y, 190, y);
      y += 10;
      
      // Verification section
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(25, 135, 84);
      doc.text('VERIFICATION', 105, y, { align: 'center' });
      y += 10;
      
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(51, 51, 51);
      addField('Digital Verification Code:', cert.digitalVerificationCode || 'N/A', true);
      
      // Generate QR code
      try {
        const qrDataUrl = await QRCode.toDataURL(
          `${window.location.origin}/verify/certificate/${cert.certificateNumber}`,
          { width: 80, margin: 1 }
        );
        doc.addImage(qrDataUrl, 'PNG', 85, y, 40, 40);
        y += 45;
        
        doc.setFontSize(9);
        doc.setTextColor(102, 102, 102);
        doc.text('Scan QR code to verify authenticity', 105, y, { align: 'center' });
      } catch (qrErr) {
        console.error('QR generation error:', qrErr);
      }
      
      // Footer
      const footerY = 270;
      doc.setFillColor(25, 135, 84);
      doc.rect(0, footerY, 210, 27, 'F');
      
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(10);
      doc.text('This is a government-issued vaccination certificate', 105, footerY + 10, { align: 'center' });
      doc.setFontSize(8);
      doc.text(`Verify at: ${window.location.origin}/verify/certificate/${cert.certificateNumber}`, 105, footerY + 18, { align: 'center' });
      
      // Save PDF
      doc.save(`certificate-${cert.certificateNumber}.pdf`);
    } catch (err) {
      console.error('Failed to generate PDF:', err);
      // Fallback to PNG if PDF fails
      generatePNG(cert);
    }
  };

  const downloadCertificate = async (certId, format) => {
    try {
      const cert = certificates.find((item) => item.id === certId);
      if (!cert) return;

      if (format === 'png') {
        generatePNG(cert);
      } else if (format === 'pdf') {
        await generatePDF(cert);
      }
    } catch (err) {
      console.error('Failed to download certificate');
    }
  };

  if (loading) {
    return (
      <div className="container py-5">
        <Skeleton height="400px" />
      </div>
    );
  }

  if (certificates.length === 0) {
    return (
      <div className="container py-5">
        <EmptyState
          title="No Certificates Available"
          description="Complete your vaccination to receive your certificate."
          actionText="View My Bookings"
          onAction={() => window.location.href = '/user/bookings'}
        />
      </div>
    );
  }

  return (
    <div className="container py-5">
      <h2 className="mb-4">My Vaccination Certificates</h2>
      <div className="row">
        {certificates.map((cert) => (
          <div key={cert.id} className="col-md-6 mb-4">
            <div className="card shadow-sm h-100">
              <div className="card-header bg-success text-white d-flex justify-content-between align-items-center">
                <h5 className="mb-0">Certificate</h5>
                <span className="badge bg-light text-success">{cert.certificateNumber}</span>
              </div>
              <div className="card-body">
                <p><strong>Vaccine:</strong> {cert.vaccineName}</p>
                <p><strong>Dose:</strong> {cert.doseNumber}</p>
                <p><strong>Issued Date:</strong> {new Date(cert.issuedAt).toLocaleDateString()}</p>
                {cert.nextDoseDate && (
                  <p><strong>Next Dose:</strong> {new Date(cert.nextDoseDate).toLocaleDateString()}</p>
                )}
                {cert.digitalVerificationCode && (
                  <p><strong>Verification Code:</strong> <code className="bg-light px-2 py-1 rounded">{cert.digitalVerificationCode}</code></p>
                )}
                <div className="mt-2">
                  <a href={`/verify/certificate?cert=${cert.certificateNumber}`} className="btn btn-outline-success btn-sm">
                    <i className="bi bi-shield-check me-1"></i> Verify Online
                  </a>
                </div>
                <div className="mt-3">
                  <div className="btn-group" role="group">
                    <button 
                      className="btn btn-primary" 
                      onClick={() => downloadCertificate(cert.id, 'png')}
                      title="Download as PNG"
                    >
                      <i className="bi bi-image me-1"></i> PNG
                    </button>
                    <button 
                      className="btn btn-success" 
                      onClick={() => downloadCertificate(cert.id, 'pdf')}
                      title="Download as PDF"
                    >
                      <i className="bi bi-file-earmark-pdf me-1"></i> PDF
                    </button>
                    <button 
                      className="btn btn-info text-white" 
                      onClick={() => handleShowQrCode(cert)}
                      title="View QR Code"
                    >
                      <i className="bi bi-qr-code me-1"></i> QR
                    </button>
                  </div>
                </div>
                {cert.qrCode && (
                  <div className="mt-3 text-center">
                    <img src={cert.qrCode} alt="QR Code" className="img-fluid" style={{ maxWidth: '150px' }} />
                    <p className="small text-muted mt-1">Scan to verify</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* QR Code Modal */}
      {showQrModal && selectedCert && (
        <div className="modal show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }} onClick={() => setShowQrModal(false)}>
          <div className="modal-dialog modal-dialog-centered" onClick={(e) => e.stopPropagation()}>
            <div className="modal-content">
              <div className="modal-header bg-success text-white">
                <h5 className="modal-title">Certificate Verification</h5>
                <button type="button" className="btn-close btn-close-white" onClick={() => setShowQrModal(false)}></button>
              </div>
              <div className="modal-body text-center">
                {qrCodeUrl && (
                  <img src={qrCodeUrl} alt="QR Code" className="img-fluid mb-3" style={{ maxWidth: '200px' }} />
                )}
                <h6 className="mb-2">Certificate No: {selectedCert.certificateNumber}</h6>
                <p className="text-muted small mb-2">{selectedCert.userName}</p>
                <p className="text-muted small mb-2">{selectedCert.vaccineName} - Dose {selectedCert.doseNumber}</p>
                <hr />
                <p className="small text-muted mb-1">Digital Verification Code:</p>
                <code className="d-block mb-3">{selectedCert.digitalVerificationCode}</code>
                <p className="small text-muted">Scan this QR code or enter the verification code at the verification page to confirm the certificate authenticity.</p>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setShowQrModal(false)}>Close</button>
                <button type="button" className="btn btn-success" onClick={() => {
                  downloadCertificate(selectedCert.id, 'pdf');
                  setShowQrModal(false);
                }}>
                  <i className="bi bi-download me-1"></i> Download PDF
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

