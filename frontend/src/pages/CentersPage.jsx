import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Helmet } from "react-helmet-async";
import { publicAPI, unwrapApiData } from "../api/client";

export default function CentersPage() {
  const [city, setCity] = useState("");
  const [centers, setCenters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uniqueCities, setUniqueCities] = useState([]);

  const load = async () => {
    setLoading(true);
    try {
      const response = await publicAPI.getCenters(city);
      const payload = unwrapApiData(response) || {};
      const data = Array.isArray(payload)
        ? payload
        : (payload.centers || payload.content || []);
      setCenters(data);
      if (data.length > 0) {
        const cities = [...new Set(data.map(c => c.city).filter(Boolean))].sort();
        setUniqueCities(cities);
      }
    } catch (error) {
      console.error("Error fetching centers:", error);
      setCenters([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  useEffect(() => {
    const handleDataUpdated = () => {
      load();
    };

    window.addEventListener("vaxzone:data-updated", handleDataUpdated);
    return () => window.removeEventListener("vaxzone:data-updated", handleDataUpdated);
  }, [city]);

  return (
    <>
      <Helmet>
        <title>Vaccination Centers - VaxZone</title>
        <meta name="description" content="Find vaccination centers near you. Browse by city and book your vaccination slot." />
        <meta property="og:title" content="Vaccination Centers - VaxZone" />
        <meta property="og:description" content="Find vaccination centers near you." />
      </Helmet>

      {/* Page Header */}
      <section className="page-header">
        <div className="container">
          <div className="row align-items-center">
            <div className="col-lg-8">
              <h1 className="mb-2">Vaccination Centers</h1>
              <p className="mb-0 opacity-75">
                Find a vaccination center near you
              </p>
            </div>
            <div className="col-lg-4 text-center text-lg-end mt-3 mt-lg-0">
              <i className="bi bi-building display-1" style={{opacity: 0.3}}></i>
            </div>
          </div>
        </div>
      </section>

      <div className="container pb-5">
        {/* Search & Filter */}
        <div className="card border-0 shadow-sm mb-4">
          <div className="card-body">
            <div className="row g-3 align-items-end">
              <div className="col-md-5">
                <label className="form-label">Search by City</label>
                <div className="input-group">
                  <span className="input-group-text bg-light">
                    <i className="bi bi-geo-alt text-muted"></i>
                  </span>
                  <input 
                    className="form-control" 
                    value={city} 
                    onChange={(e) => setCity(e.target.value)} 
                    placeholder="Enter city name"
                  />
                </div>
              </div>
              <div className="col-md-3">
                <label className="form-label">Quick Filters</label>
                <select className="form-select" onChange={(e) => setCity(e.target.value)} value={city}>
                  <option value="">All Cities</option>
                  {uniqueCities.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
              <div className="col-md-2 d-grid">
                <button className="btn btn-primary" onClick={load}>
                  <i className="bi bi-search me-2"></i>Search
                </button>
              </div>
              <div className="col-md-2 d-grid">
                <button className="btn btn-outline-secondary" onClick={() => { setCity(""); load(); }}>
                  <i className="bi bi-arrow-counterclockwise me-2"></i>Reset
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Results */}
        {loading ? (
          <div className="text-center py-5">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-3 text-muted">Loading vaccination centers...</p>
          </div>
        ) : centers.length > 0 ? (
          <>
            <div className="d-flex justify-content-between align-items-center mb-4">
              <p className="text-muted mb-0">
                Showing <strong>{centers.length}</strong> center{centers.length !== 1 ? 's' : ''}
                {city && <span> in <strong>{city}</strong></span>}
              </p>
              <Link to="/drives" className="btn btn-outline-primary btn-sm">
                <i className="bi bi-calendar-check me-2"></i>View Drives
              </Link>
            </div>
            <div className="row g-4">
              {centers.map((c, index) => (
                <div className="col-md-6 col-lg-4 fade-in" key={c.id} style={{animationDelay: `${index * 0.1}s`}}>
                  <div className="center-card h-100">
                    <div className="card-header">
                      <h5 className="mb-0">
                        <i className="bi bi-building me-2"></i>
                        {c.name}
                      </h5>
                    </div>
                    <div className="card-body">
                      <div className="center-info">
                        <div className="info-item">
                          <i className="bi bi-geo-alt"></i>
                          <span>{c.address}</span>
                        </div>
                        <div className="info-item">
                          <i className="bi bi-map"></i>
                          <span>{c.city}</span>
                        </div>
                        {c.phone && (
                          <div className="info-item">
                            <i className="bi bi-telephone"></i>
                            <span>{c.phone}</span>
                          </div>
                        )}
                        {c.email && (
                          <div className="info-item">
                            <i className="bi bi-envelope"></i>
                            <span>{c.email}</span>
                          </div>
                        )}
                      </div>
                      <div className="mt-3 d-flex flex-wrap gap-2">
                        <span className="badge bg-success">
                          <i className="bi bi-people me-1"></i>
                          {c.dailyCapacity}/day
                        </span>
                        {c.isActive !== false && (
                          <span className="badge bg-info">
                            <i className="bi bi-check-circle me-1"></i>
                            Active
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="card-footer">
                      <Link to={`/drives?city=${encodeURIComponent(c.city)}`} className="btn btn-primary w-100">
                        <i className="bi bi-calendar-plus me-2"></i>View Drives
                      </Link>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </>
        ) : (
          <div className="empty-state">
            <i className="bi bi-building"></i>
            <h5>No Centers Found</h5>
            <p>Try a different city or check back later.</p>
            <div className="d-flex gap-2 justify-content-center">
              <button className="btn btn-primary" onClick={() => { setCity(""); load(); }}>
                View All Centers
              </button>
              <Link to="/contact" className="btn btn-outline-primary">
                Contact Us
              </Link>
            </div>
          </div>
        )}

        {/* Info Section */}
        <div className="mt-5">
          <div className="row g-4">
            <div className="col-md-4">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body text-center p-4">
                  <div className="icon-wrapper">
                    <i className="bi bi-shield-check"></i>
                  </div>
                  <h5 className="fw-bold mt-3">Safe & Secure</h5>
                  <p className="text-muted small mb-0">
                    All centers follow strict safety protocols and guidelines.
                  </p>
                </div>
              </div>
            </div>
            <div className="col-md-4">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body text-center p-4">
                  <div className="icon-wrapper">
                    <i className="bi bi-clock"></i>
                  </div>
                  <h5 className="fw-bold mt-3">Flexible Hours</h5>
                  <p className="text-muted small mb-0">
                    Extended hours and weekend appointments available.
                  </p>
                </div>
              </div>
            </div>
            <div className="col-md-4">
              <div className="card border-0 shadow-sm h-100">
                <div className="card-body text-center p-4">
                  <div className="icon-wrapper">
                    <i className="bi bi-person-check"></i>
                  </div>
                  <h5 className="fw-bold mt-3">Expert Staff</h5>
                  <p className="text-muted small mb-0">
                    Trained healthcare professionals for best experience.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* CTA */}
        <div className="mt-5">
          <div className="card border-0 shadow-sm bg-primary text-white">
            <div className="card-body py-4 text-center">
              <h4 className="fw-bold mb-2">Ready to Get Vaccinated?</h4>
              <p className="mb-3">Browse available drives and book your appointment today.</p>
              <Link to="/drives" className="btn btn-light btn-lg">
                <i className="bi bi-calendar-check me-2"></i>Find a Drive
              </Link>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
