import React, { useState, useEffect } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { certificateAPI, unwrapApiData } from "../api/client";
import QRCode from "qrcode";
import ModalPopup from "../components/ModalPopup";
import SearchInput from "../components/SearchInput";
import Seo from "../components/Seo";

export default function VerifyCertificatePage() {
  const [searchParams] = useSearchParams();
  const [certificateNumber, setCertificateNumber] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [certificate, setCertificate] = useState(null);
  const [verified, setVerified] = useState(false);
  const [qrCodeUrl, setQrCodeUrl] = useState(null);
  const [showCopiedModal, setShowCopiedModal] = useState(false);

  // Check if certificate number is in URL params (from QR code scan)
  useEffect(() => {
    const certNum = searchParams.get("cert");
    if (certNum) {
      setCertificateNumber(certNum);
      handleVerify(certNum);
    }
  }, [searchParams]);

  const generateQRCode = async (cert) => {
    try {
      const verificationUrl = `${window.location.origin}/verify/certificate?cert=${cert.certificateNumber}`;
      const qrDataUrl = await QRCode.toDataURL(verificationUrl, {
        width: 150,
        margin: 2,
      });
      return qrDataUrl;
    } catch (err) {
      console.error("Failed to generate QR code:", err);
      return null;
    }
  };

  const handleVerify = async (certNum = certificateNumber) => {
    if (!certNum || !certNum.trim()) {
      setError("Please enter a certificate number");
      return;
    }

    setLoading(true);
    setError("");
    setCertificate(null);
    setVerified(false);

    try {
      const response = await certificateAPI.verifyCertificate(certNum.trim());
      const payload = unwrapApiData(response);

      if (payload) {
        setCertificate(payload);
        setVerified(true);
        const qrUrl = await generateQRCode(payload);
        setQrCodeUrl(qrUrl);
      }
    } catch (err) {
      setLoading(false);
      
      if (err.response) {
        const status = err.response.status;
        if (status === 404) {
          setError("Certificate not found. Please check the certificate number and try again.");
        } else if (status === 500) {
          setError("Server error. Please try again later.");
        } else {
          setError("Failed to verify certificate. Please try again.");
        }
      } else if (err.request) {
        setError("Network error. Please check your internet connection.");
      } else {
        setError("Failed to verify certificate. Please try again.");
      }
      console.error("Verification error:", err);
      return;
    }
    
    setLoading(false);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    handleVerify();
  };

  const copyToClipboard = () => {
    navigator.clipboard.writeText(certificate?.digitalVerificationCode || certificate?.certificateNumber);
    setShowCopiedModal(true);
  };

  return (
    <>
      <Seo
        title="Verify Vaccination Certificate | VaxZone"
        description="Verify the authenticity of a vaccination certificate using its certificate number and QR-ready validation flow."
        path="/verify/certificate"
      />

      {/* Page Header */}
      <section className="page-header">
        <div className="container">
          <div className="row align-items-center">
            <div className="col-lg-8">
              <h1 className="mb-2">Verify Certificate</h1>
              <p className="mb-0 opacity-75">
                Enter your certificate number to verify its authenticity
              </p>
            </div>
            <div className="col-lg-4 text-center text-lg-end mt-3 mt-lg-0">
              <i className="bi bi-shield-check display-1 page-header__icon"></i>
            </div>
          </div>
        </div>
      </section>

      <div className="container py-5">
        <div className="row justify-content-center">
          <div className="col-lg-8">
            {/* Verification Form */}
            <div className="card border-0 shadow-sm mb-4">
              <div className="card-body p-4">
                <h4 className="fw-bold mb-4">Certificate Verification</h4>
                
                <form onSubmit={handleSubmit}>
                  <div className="mb-3">
                    <label htmlFor="certNumber" className="form-label">
                      Certificate Number
                    </label>
                    <div className="search-action-row">
                      <SearchInput
                        id="certNumber"
                        value={certificateNumber}
                        onChange={setCertificateNumber}
                        placeholder="Search certificate number (e.g., CERT-2024-001234)"
                        icon="search"
                        loading={loading}
                        disabled={loading}
                        onClear={() => {
                          setCertificateNumber("");
                          setError("");
                        }}
                      />
                      <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={loading}
                      >
                        {loading ? (
                          <>
                            <span className="spinner-border spinner-border-sm me-2" role="status"></span>
                            Verifying...
                          </>
                        ) : (
                          <>
                            <i className="bi bi-search me-2"></i> Verify
                          </>
                        )}
                      </button>
                    </div>
                    <div className="form-text">
                      You can find the certificate number on your vaccination certificate.
                    </div>
                  </div>
                </form>

                {error && (
                  <div className="alert alert-danger">
                    <i className="bi bi-exclamation-triangle me-2"></i>
                    {error}
                  </div>
                )}
              </div>
            </div>

            {/* Verified Certificate Display */}
            {verified && certificate && (
              <div className="card border-0 shadow-sm mb-4 fade-in">
                <div className="card-header bg-success text-white d-flex align-items-center">
                  <i className="bi bi-check-circle-fill me-2"></i>
                  <span className="fw-bold">Certificate Verified Successfully</span>
                </div>
                <div className="card-body p-4">
                  <div className="row">
                    {/* Certificate Details */}
                    <div className="col-md-7">
                      <h5 className="fw-bold mb-3">Certificate Details</h5>
                      
                      <table className="table table-borderless">
                        <tbody>
                          <tr>
                            <td className="text-muted fw-bold" style={{width: '40%'}}>Certificate No:</td>
                            <td className="fw-bold">{certificate.certificateNumber}</td>
                          </tr>
                          <tr>
                            <td className="text-muted fw-bold">Beneficiary Name:</td>
                            <td>{certificate.userName}</td>
                          </tr>
                          <tr>
                            <td className="text-muted fw-bold">Email:</td>
                            <td>{certificate.userEmail}</td>
                          </tr>
                          <tr>
                            <td className="text-muted fw-bold">Vaccine Name:</td>
                            <td>{certificate.vaccineName}</td>
                          </tr>
                          <tr>
                            <td className="text-muted fw-bold">Dose Number:</td>
                            <td>{certificate.doseNumber ? `Dose ${certificate.doseNumber}` : 'N/A'}</td>
                          </tr>
                          <tr>
                            <td className="text-muted fw-bold">Vaccination Center:</td>
                            <td>{certificate.centerName}</td>
                          </tr>
                          <tr>
                            <td className="text-muted fw-bold">Drive:</td>
                            <td>{certificate.driveTitle}</td>
                          </tr>
                          <tr>
                            <td className="text-muted fw-bold">Date of Vaccination:</td>
                            <td>{certificate.slotDateTime ? new Date(certificate.slotDateTime).toLocaleDateString() : 'N/A'}</td>
                          </tr>
                          <tr>
                            <td className="text-muted fw-bold">Date of Issue:</td>
                            <td>{certificate.issuedAt ? new Date(certificate.issuedAt).toLocaleDateString() : 'N/A'}</td>
                          </tr>
                          {certificate.nextDoseDate && (
                            <tr>
                              <td className="text-muted fw-bold">Next Dose Date:</td>
                              <td>{new Date(certificate.nextDoseDate).toLocaleDateString()}</td>
                            </tr>
                          )}
                          <tr>
                            <td className="text-muted fw-bold">Digital Verification Code:</td>
                            <td>
                              <code className="bg-light px-2 py-1 rounded">{certificate.digitalVerificationCode}</code>
                              <button 
                                className="btn btn-sm btn-link ms-2" 
                                onClick={copyToClipboard}
                                title="Copy to clipboard"
                              >
                                <i className="bi bi-clipboard"></i>
                              </button>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                    
                    {/* QR Code */}
                    <div className="col-md-5 text-center">
                      <div className="bg-light p-4 rounded">
                        {qrCodeUrl && (
                          <img src={qrCodeUrl} alt="Verification QR Code" className="img-fluid mb-3" style={{maxWidth: '150px'}} />
                        )}
                        <p className="small text-muted mb-0">Scan to verify</p>
                      </div>
                      
                      <div className="mt-3">
                        <p className="small text-muted">
This certificate is issued by a registered vaccination center and is verified through the VaxZone system.
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* How to Find Certificate Number */}
            <div className="card border-0 shadow-sm">
              <div className="card-body p-4">
                <h5 className="fw-bold mb-3">
                  <i className="bi bi-question-circle text-primary me-2"></i>
                  How to Find Your Certificate Number
                </h5>
                <ul className="text-muted">
                  <li className="mb-2">Check your vaccination certificate document</li>
                  <li className="mb-2">Look for "Certificate No:" or "Cert Number" on the certificate</li>
                  <li className="mb-2">The certificate number usually starts with "CERT-" followed by digits</li>
                  <li className="mb-2">You can also find it in your booking confirmation email</li>
                </ul>
                
                <hr />
                
                <h5 className="fw-bold mb-3">
                  <i className="bi bi-info-circle text-primary me-2"></i>
                  Need Help?
                </h5>
                <p className="text-muted">
                  If you're having trouble verifying your certificate, please contact the vaccination center where you received your dose or reach out to our support team.
                </p>
                <Link to="/contact" className="btn btn-outline-primary">
                  <i className="bi bi-envelope me-2"></i> Contact Support
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
      <ModalPopup
        show={showCopiedModal}
        title="Copied"
        body="The verification code has been copied to your clipboard."
        confirmLabel="Done"
        onConfirm={() => setShowCopiedModal(false)}
        onCancel={() => setShowCopiedModal(false)}
      />
    </>
  );
}
