import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Helmet } from "react-helmet-async";
import { publicAPI, unwrapApiData } from "../api/client";

export default function HomePage() {
  const [stats, setStats] = useState({ centers: 0, drives: 0, bookings: 0 });
  const [recentDrives, setRecentDrives] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = async () => {
    try {
      console.log("Fetching home page summary and drives");
      const [statsRes, drivesRes] = await Promise.all([
        publicAPI.getSummary(),
        publicAPI.getDrives()
      ]);
      const summaryData = unwrapApiData(statsRes) || {};
      console.log("Home summary response:", summaryData);
      setStats({
        centers: summaryData.totalCenters || summaryData.centersCount || 0,
        drives: summaryData.activeDrives || summaryData.drivesCount || 0,
        bookings: summaryData.availableSlots || 0
      });
      const drivesPayload = unwrapApiData(drivesRes) || {};
      console.log("Home drives response:", drivesPayload);
      const drivesData = Array.isArray(drivesPayload)
        ? drivesPayload
        : (drivesPayload.drives || []);
      const mappedDrives = drivesData.map(drive => {
        return {
          ...drive,
          name: drive.title,
          date: drive.driveDate,
          centerName: drive.center?.name || drive.centerName,
          availableSlots: drive.availableSlots ?? drive.totalSlots ?? 0,
          totalSlots: drive.totalSlots || 0,
          startTime: drive.startTime || 'N/A',
          endTime: drive.endTime || 'N/A'
        };
      });
      setRecentDrives(mappedDrives.slice(0, 6));
      setError(null);
    } catch (error) {
      console.error("Error fetching home data:", error);
      setError(error.message || 'Failed to load data. Please check your connection.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();

    const handleRefresh = () => fetchData();
    const handleVisibilityRefresh = () => {
      if (document.visibilityState === "visible") {
        fetchData();
      }
    };
    const intervalId = window.setInterval(fetchData, 30000);
    window.addEventListener('focus', handleRefresh);
    window.addEventListener('vaxzone:data-updated', handleRefresh);
    document.addEventListener('visibilitychange', handleVisibilityRefresh);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener('focus', handleRefresh);
      window.removeEventListener('vaxzone:data-updated', handleRefresh);
      document.removeEventListener('visibilitychange', handleVisibilityRefresh);
    };
  }, []);

  return (
    <>
      <Helmet>
        <title>VaxZone - Smart Vaccination Scheduling System</title>
        <meta name="description" content="Book your vaccination appointments easily with VaxZone. Find vaccination drives, centers near you, and manage your bookings online." />
        <meta property="og:title" content="VaxZone - Smart Vaccination Scheduling" />
        <meta property="og:description" content="Book your vaccination appointments easily with our smart scheduling system." />
      </Helmet>

      {/* Hero Section */}
      <section className="hero-section">
        <div className="container">
          <div className="row align-items-center">
            <div className="col-lg-7">
              <div className="fade-in">
                <span className="badge bg-primary bg-opacity-25 text-primary px-3 py-2 mb-3">
                  <i className="bi bi-shield-check me-1"></i> Trusted by 50,000+ Citizens
                </span>
                <h1 className="mb-4">
                  Get Vaccinated Safely & Easily
                </h1>
                <p className="lead mb-4">
                  VaxZone is your complete vaccination management platform.
                  Book appointments, track your status, and get real-time notifications — all in one place.
                </p>
                <div className="d-flex gap-3 flex-wrap mb-5">
                  <Link to="/register" className="btn btn-primary btn-lg">
                    <i className="bi bi-person-plus me-2"></i>Get Started Free
                  </Link>
                  <Link to="/drives" className="btn btn-outline-primary btn-lg">
                    <i className="bi bi-calendar-check me-2"></i>Browse Drives
                  </Link>
                </div>
                
                {/* Live Stats */}
                <div className="d-flex gap-4 flex-wrap">
                  <div className="stats-card" style={{minWidth: '120px'}}>
                    <div className="stat-number">{stats.centers || 0}</div>
                    <div className="stat-label">Centers</div>
                  </div>
                  <div className="stats-card bg-success" style={{minWidth: '120px'}}>
                    <div className="stat-number">{stats.drives}</div>
                    <div className="stat-label">Active Drives</div>
                  </div>
                  <div className="stats-card bg-info" style={{minWidth: '120px'}}>
                    <div className="stat-number">{stats.bookings?.toLocaleString() || 0}</div>
                    <div className="stat-label">Available Slots</div>
                  </div>
                </div>
              </div>
            </div>
            <div className="col-lg-5 text-center mt-5 mt-lg-0">
              <div className="position-relative fade-in stagger-2">
                {/* Floating Cards */}
                <div className="position-absolute" style={{top: '-20px', right: '20px', animation: 'float 6s ease-in-out infinite'}}>
                  <div className="card shadow-lg p-3" style={{width: '160px'}}>
                    <div className="d-flex align-items-center gap-2">
                      <div className="bg-success rounded-circle p-2">
                        <i className="bi bi-check-lg text-white"></i>
                      </div>
                      <div>
                        <small className="text-muted d-block">Status</small>
                        <strong>Verified</strong>
                      </div>
                    </div>
                  </div>
                </div>
                <div className="position-absolute" style={{bottom: '20px', left: '-10px', animation: 'float 8s ease-in-out infinite reverse'}}>
                  <div className="card shadow-lg p-3" style={{width: '150px'}}>
                    <div className="d-flex align-items-center gap-2">
                      <div className="bg-primary rounded-circle p-2">
                        <i className="bi bi-calendar-check text-white"></i>
                      </div>
                      <div>
                        <small className="text-muted d-block">Next Slot</small>
                        <strong>Tomorrow</strong>
                      </div>
                    </div>
                  </div>
                </div>
                
                {/* Main Icon */}
                <div className="position-relative d-inline-block">
                  <div style={{
                    width: '300px', 
                    height: '300px', 
                    background: 'linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%)',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    boxShadow: '0 25px 50px -12px rgba(14, 165, 233, 0.4)'
                  }}>
                    <i className="bi bi-shield-check text-white" style={{fontSize: '120px'}}></i>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-5 bg-white">
        <div className="container">
          <div className="text-center mb-5">
            <h2 className="section-title">Why Choose VaxZone?</h2>
            <p className="section-subtitle">Everything you need for seamless vaccination management</p>
          </div>
          <div className="row g-4">
            <div className="col-md-4">
              <div className="feature-card">
                <div className="icon-wrapper">
                  <i className="bi bi-calendar-check"></i>
                </div>
                <h5>Easy Online Booking</h5>
                <p>
                  Schedule your vaccination appointments from anywhere, anytime. 
                  Choose your preferred date, time, and center.
                </p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="feature-card">
                <div className="icon-wrapper">
                  <i className="bi bi-bell"></i>
                </div>
                <h5>Real-time Notifications</h5>
                <p>
                  Stay updated with instant notifications about booking confirmations, 
                  reminders, and drive updates.
                </p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="feature-card">
                <div className="icon-wrapper">
                  <i className="bi bi-graph-up"></i>
                </div>
                <h5>Admin Dashboard</h5>
                <p>
                  Powerful dashboard for administrators to manage drives, 
                  monitor bookings, and analyze statistics.
                </p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="feature-card">
                <div className="icon-wrapper">
                  <i className="bi bi-geo-alt"></i>
                </div>
                <h5>Find Nearby Centers</h5>
                <p>
                  Locate vaccination centers near you with our easy-to-use 
                  search and filter by city.
                </p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="feature-card">
                <div className="icon-wrapper">
                  <i className="bi bi-shield-lock"></i>
                </div>
                <h5>Secure & Private</h5>
                <p>
                  Your personal information is protected with enterprise-grade 
                  security and encrypted data storage.
                </p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="feature-card">
                <div className="icon-wrapper">
                  <i className="bi bi-clock-history"></i>
                </div>
                <h5>Track History</h5>
                <p>
                  View your complete vaccination history and download 
                  certificates for your records.
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Recent Drives Section */}
      <section className="py-5 bg-light">
        <div className="container">
          <div className="d-flex justify-content-between align-items-center mb-4">
            <div>
              <h2 className="section-title mb-1">Upcoming Vaccination Drives</h2>
              <p className="text-muted mb-0">Find and book available slots near you</p>
            </div>
            <Link to="/drives" className="btn btn-outline-primary">
              View All <i className="bi bi-arrow-right ms-1"></i>
            </Link>
          </div>
          
{loading ? (
            <div className="text-center py-5">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Loading...</span>
              </div>
            </div>
          ) : error ? (
            <div className="empty-state text-center py-5">
              <i className="bi bi-wifi-off text-danger" style={{fontSize: '4rem'}}></i>
              <h5 className="mt-3">Unable to Load Data</h5>
              <p className="text-muted">{error}</p>
              <button className="btn btn-primary" onClick={() => window.location.reload()}>
                Retry
              </button>
            </div>
          ) : recentDrives.length > 0 ? (
            <div className="row g-4">
              {recentDrives.map((drive, index) => (
                <div key={drive.id} className={`col-md-6 col-lg-4 fade-in stagger-${index + 1}`}>
                  <div className="drive-card h-100">
                    <div className="card-header d-flex justify-content-between align-items-center">
                      <h5 className="mb-0" style={{fontSize: '1rem'}}>{drive.name}</h5>
                      {drive.availableSlots > 0 ? (
                        <span className="badge bg-white text-primary">{drive.availableSlots} left</span>
                      ) : (
                        <span className="badge bg-danger">Full</span>
                      )}
                    </div>
                    <div className="card-body">
                      <div className="info-item">
                        <i className="bi bi-calendar-event"></i>
                        <span>{new Date(drive.date).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })}</span>
                      </div>
                      <div className="info-item">
                        <i className="bi bi-clock"></i>
                        <span>{drive.startTime} - {drive.endTime}</span>
                      </div>
                      <div className="info-item">
                        <i className="bi bi-building"></i>
                        <span>{drive.centerName}</span>
                      </div>
                      <div className="info-item">
                        <i className="bi bi-person-badge"></i>
                        <span>Age: {drive.minAge}-{drive.maxAge} years</span>
                      </div>
                      
                      {/* Progress Bar */}
                      <div className="mt-3">
                        <div className="d-flex justify-content-between mb-1">
                          <small className="text-muted">Capacity</small>
                          <small className="text-muted">{drive.availableSlots}/{drive.totalSlots}</small>
                        </div>
                        <div className="slots-progress">
                          <div 
                            className="progress-bar" 
                            style={{
                              width: `${drive.totalSlots > 0 ? ((drive.totalSlots - drive.availableSlots) / drive.totalSlots) * 100 : 0}%`
                            }}
                          ></div>
                        </div>
                      </div>
                    </div>
                    <div className="card-footer bg-white border-top-0 pt-0">
                      {drive.availableSlots > 0 ? (
                        <Link to={`/drives?book=${drive.id}`} className="btn btn-primary w-100">
                          <i className="bi bi-bookmark-plus me-2"></i>Book Now
                        </Link>
                      ) : (
                        <button className="btn btn-secondary w-100" disabled>
                          <i className="bi bi-x-circle me-2"></i>No Slots Available
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <i className="bi bi-calendar-x"></i>
              <h5>No Upcoming Drives</h5>
              <p>No upcoming drives found. Check back soon for new vaccination drives!</p>
              <Link to="/drives" className="btn btn-primary">Browse All Drives</Link>
            </div>
          )}
        </div>
      </section>

      {/* How It Works */}
      <section className="py-5 bg-white">
        <div className="container">
          <div className="text-center mb-5">
            <h2 className="section-title">How It Works</h2>
            <p className="section-subtitle">Get vaccinated in three simple steps</p>
          </div>
          <div className="row g-4">
            <div className="col-md-4 text-center">
              <div className="mb-4">
                <div style={{
                  width: '80px', 
                  height: '80px', 
                  margin: '0 auto',
                  background: 'linear-gradient(135deg, var(--primary-light) 0%, #e0f2fe 100%)',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <span className="h3 mb-0 text-primary fw-bold">1</span>
                </div>
              </div>
              <h5 className="fw-bold">Create Account</h5>
              <p className="text-muted">Register for free and create your personal profile</p>
            </div>
            <div className="col-md-4 text-center">
              <div className="mb-4">
                <div style={{
                  width: '80px', 
                  height: '80px', 
                  margin: '0 auto',
                  background: 'linear-gradient(135deg, var(--primary-light) 0%, #e0f2fe 100%)',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <span className="h3 mb-0 text-primary fw-bold">2</span>
                </div>
              </div>
              <h5 className="fw-bold">Book Your Slot</h5>
              <p className="text-muted">Find a drive near you and book your vaccination slot</p>
            </div>
            <div className="col-md-4 text-center">
              <div className="mb-4">
                <div style={{
                  width: '80px', 
                  height: '80px', 
                  margin: '0 auto',
                  background: 'linear-gradient(135deg, var(--primary-light) 0%, #e0f2fe 100%)',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <span className="h3 mb-0 text-primary fw-bold">3</span>
                </div>
              </div>
              <h5 className="fw-bold">Get Vaccinated</h5>
              <p className="text-muted">Visit the center and get your vaccination</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-5" style={{
        background: 'linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%)',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'url("data:image/svg+xml,%3Csvg width=\'60\' height=\'60\' viewBox=\'0 0 60 60\' xmlns=\'http://www.w3.org/2000/svg\'%3E%3Cg fill=\'none\' fill-rule=\'evenodd\'%3E%3Cg fill=\'%23ffffff\' fill-opacity=\'0.05\'%3E%3Cpath d=\'M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z\'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")'
        }}></div>
        <div className="container text-center position-relative">
          <h2 className="fw-bold mb-3 text-white">Ready to Get Started?</h2>
          <p className="mb-4 fs-5 text-white-50">Join thousands of people who have already booked their vaccination slots</p>
          <div className="d-flex gap-3 justify-content-center flex-wrap">
            <Link to="/register" className="btn btn-light btn-lg">
              <i className="bi bi-person-plus me-2"></i>Register Now
            </Link>
            <Link to="/centers" className="btn btn-outline-light btn-lg">
              <i className="bi bi-geo-alt me-2"></i>Find Centers
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
