import React from "react";
import { Helmet } from "react-helmet-async";

const PrivacyPolicyPage = () => {
  return (
    <>
      <Helmet>
        <title>Privacy Policy | VaxZone</title>
        <meta name="description" content="Privacy Policy for VaxZone - Learn how we protect your personal information and data." />
        <meta name="robots" content="index, follow" />
        <link rel="canonical" href="https://vaxzone.com/privacy-policy" />
      </Helmet>
      
      <div className="privacy-policy-page">
        <div className="container py-5">
          <div className="row justify-content-center">
            <div className="col-lg-10">
              <div className="card shadow-sm border-0">
                <div className="card-body p-5">
                  <h1 className="display-5 fw-bold text-primary mb-4">Privacy Policy</h1>
                  <p className="text-muted mb-4">Last updated: {new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}</p>
                  <hr className="my-4" />
                  
                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">1. Introduction</h2>
                    <p className="text-secondary">
                      VaxZone ("we," "our," or "us") is committed to protecting your privacy. 
                      This Privacy Policy explains how your personal information is collected, used, disclosed, and 
                      safeguarded when you use our vaccination booking platform.
                    </p>
                  </section>

                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">2. Information We Collect</h2>
                    <div className="ps-3">
                      <h3 className="h6 fw-bold text-dark mb-2">Personal Information:</h3>
                      <ul className="text-secondary">
                        <li>Full name</li>
                        <li>Email address</li>
                        <li>Phone number</li>
                        <li>Date of birth / Age</li>
                        <li>Government-issued ID (for verification)</li>
                        <li>Medical information (if relevant to vaccination)</li>
                      </ul>
                    </div>
                  </section>

                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">3. How We Use Your Information</h2>
                    <p className="text-secondary">We use your information to:</p>
                    <ul className="text-secondary ps-3">
                      <li>Process your vaccination booking requests</li>
                      <li>Send booking confirmations and reminders</li>
                      <li>Provide customer support</li>
                      <li>Improve our services</li>
                      <li>Comply with legal obligations</li>
                      <li>Send important notifications</li>
                    </ul>
                  </section>

                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">4. Information Sharing</h2>
                    <p className="text-secondary">
                      We do not sell, trade, or rent your personal information to third parties. 
                      We may share your information with healthcare providers and government health authorities as required by law.
                    </p>
                  </section>

                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">5. Data Security</h2>
                    <p className="text-secondary">
                      We implement appropriate technical and organizational security measures to protect your personal information, including SSL encryption, secure password hashing, and access controls.
                    </p>
                  </section>

                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">6. Your Rights</h2>
                    <p className="text-secondary">You have the right to access, correct, request deletion of your data, and object to processing.</p>
                  </section>

                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">7. Contact Us</h2>
                    <p className="text-secondary">If you have questions about this Privacy Policy, please contact us at:</p>
                    <div className="bg-light p-4 rounded">
                      <p className="mb-1"><strong>VaxZone</strong></p>
                      <p className="mb-1">Email: vaxzone.vaccine@gmail.com</p>
                      <p className="mb-0">Phone: +91 9631376436</p>
                    </div>
                  </section>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default PrivacyPolicyPage;

