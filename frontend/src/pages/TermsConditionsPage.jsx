import React from "react";
import { Helmet } from "react-helmet-async";

const TermsConditionsPage = () => {
  return (
    <>
      <Helmet>
        <title>Terms and Conditions | VaxZone</title>
        <meta name="description" content="Terms and Conditions for VaxZone - Read our terms of service and user agreement." />
        <meta name="robots" content="index, follow" />
        <link rel="canonical" href="https://vaxzone.com/terms-conditions" />
      </Helmet>
      
      <div className="terms-conditions-page">
        <div className="container py-5">
          <div className="row justify-content-center">
            <div className="col-lg-10">
              <div className="card shadow-sm border-0">
                <div className="card-body p-5">
                  <h1 className="display-5 fw-bold text-primary mb-4">Terms and Conditions</h1>
                  <p className="text-muted mb-4">Last updated: {new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}</p>
                  <hr className="my-4" />
                  
                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">1. Acceptance of Terms</h2>
                    <p className="text-secondary">
                      By accessing and using VaxZone ("the Platform"), you accept and agree 
                      to be bound by the terms and provision of this agreement.
                    </p>
                  </section>

                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">2. Description of Service</h2>
                    <p className="text-secondary">
                      VaxZone provides an online platform for viewing vaccination drives and centers, 
                      booking vaccination appointments, managing booking records, and receiving notifications.
                    </p>
                  </section>

                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">3. User Registration</h2>
                    <p className="text-secondary">
                      To use our services, you must provide accurate information, be at least 13 years of age, 
                      and maintain the security of your account.
                    </p>
                  </section>

                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">4. Booking Rules</h2>
                    <ul className="text-secondary ps-3">
                      <li>One booking per person per time slot</li>
                      <li>Provide accurate health information</li>
                      <li>Cancel or reschedule at least 24 hours in advance</li>
                      <li>Bring valid ID and any required documents</li>
                    </ul>
                  </section>

                  <section className="mb-5">
                    <h2 className="h4 fw-bold text-dark mb-3">5. Contact Information</h2>
                    <p className="text-secondary">If you have any questions about these Terms, please contact us at:</p>
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

export default TermsConditionsPage;

