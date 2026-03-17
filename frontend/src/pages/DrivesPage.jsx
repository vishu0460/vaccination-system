import React, { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Helmet } from "react-helmet-async";
import api from "../api/client";

export default function DrivesPage() {
  const [searchParams] = useSearchParams();
  const [drives, setDrives] = useState([]);
  const [summary, setSummary] = useState({ totalCenters: 0, activeDrives: 0, availableSlots: 0 });
  const [filters, setFilters] = useState({ city: "", fromDate: "", age: "" });
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState("grid");

  const load = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filters.city.trim()) params.set("city", filters.city.trim());
      if (filters.fromDate) params.set("fromDate", filters.fromDate);
      if (filters.age) params.set("age", filters.age);

      const drivesUrl = params.toString() ? `/public/drives?${params.toString()}` : "/public/drives";
      const [drivesRes, summaryRes] = await Promise.all([
        api.get(drivesUrl),
        api.get("/public/summary")
      ]);
      // Map backend response to frontend format
      const drivesPayload = drivesRes.data;
      const drivesData = Array.isArray(drivesPayload)
        ? drivesPayload
        : (drivesPayload?.drives || []);
      const mappedDrives = drivesData.map(drive => {
        // Calculate slots from the drive's slots array
        const slots = drive.slots || [];
        const totalCapacity = slots.reduce((sum, slot) => sum + (slot.capacity || 0), 0);
        const totalBooked = slots.reduce((sum, slot) => sum + (slot.bookedCount || 0), 0);
        const hasSlots = slots.length > 0;
        
        return {
          ...drive,
          name: drive.title,
          date: drive.driveDate,
          centerName: drive.center?.name,
          hasSlots: hasSlots,  // Flag to check if drive has any slots
          availableSlots: totalCapacity - totalBooked,
          totalSlots: totalCapacity,
          startTime: hasSlots ? new Date(slots[0].startTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : 'N/A',
          endTime: hasSlots ? new Date(slots[slots.length - 1].endTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : 'N/A'
        };
      });
      setDrives(mappedDrives);
      setSummary(summaryRes.data || summary);
    } catch (error) {
      console.error("Error fetching drives:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    const bookId = searchParams.get("book");
    if (bookId && drives.length > 0) {
      const drive = drives.find(d => d.id === parseInt(bookId));
      if (drive) {
        handleBookSlot(drive);
      }
    }
  }, [searchParams, drives]);

  const handleBookSlot = (drive) => {
    window.location.href = `/login?redirect=/drives?book=${drive.id}`;
  };

  const eventJsonLd = useMemo(() => ({
    "@context": "https://schema.org",
    "@type": "ItemList",
    itemListElement: drives.map((d, i) => ({
      "@type": "Event",
      position: i + 1,
      name: d.title || d.name,
      startDate: d.driveDate || d.date,
      location: {
        "@type": "Place",
        name: d.center?.name || d.centerName,
        address: d.center?.address
      }
    }))
  }), [drives]);

  return (
    <>
      <Helmet>
        <title>Vaccination Drives - VaxZone</title>
        <meta name="description" content="Find and book upcoming vaccination drives near you. Browse by city, date, and availability." />
        <meta property="og:title" content="Vaccination Drives - VaxZone" />
        <meta property="og:description" content="Find and book upcoming vaccination drives near you." />
        <script type="application/ld+json">{JSON.stringify(eventJsonLd)}</script>
      </Helmet>

      {/* Page Header */}
      <section className="page-header">
        <div className="container">
          <div className="row align-items-center">
            <div className="col-lg-8">
              <h1 className="mb-2">Vaccination Drives</h1>
              <p className="mb-0 opacity-75">
                Find and book your vaccination slot at a drive near you
              </p>
            </div>
            <div className="col-lg-4 text-center text-lg-end mt-3 mt-lg-0">
              <i className="bi bi-calendar-event display-1" style={{opacity: 0.3}}></i>
            </div>
          </div>
        </div>
      </section>

      <div className="container pb-5">
        {/* Stats Cards */}
        <div className="row g-3 mb-4">
          <div className="col-md-4">
            <div className="stats-card">
              <div className="stat-number">{summary.activeDrives}</div>
              <div className="stat-label">Active Drives</div>
            </div>
          </div>
          <div className="col-md-4">
            <div className="stats-card bg-success">
              <div className="stat-number">{summary.totalCenters}</div>
              <div className="stat-label">Centers</div>
            </div>
          </div>
          <div className="col-md-4">
            <div className="stats-card bg-info">
              <div className="stat-number">{summary.availableSlots}</div>
              <div className="stat-label">Available Slots</div>
            </div>
          </div>
        </div>

        {/* Filters */}
        <div className="card border-0 shadow-sm mb-4">
          <div className="card-body">
            <div className="row g-3 align-items-end">
              <div className="col-md-3">
                <label className="form-label">City</label>
                <div className="input-group">
                  <span className="input-group-text bg-light">
                    <i className="bi bi-geo-alt text-muted"></i>
                  </span>
                  <input
                    className="form-control"
                    placeholder="Search by city"
                    value={filters.city}
                    onChange={(e) => setFilters({ ...filters, city: e.target.value })}
                  />
                </div>
              </div>
              <div className="col-md-3">
                <label className="form-label">Date</label>
                <div className="input-group">
                  <span className="input-group-text bg-light">
                    <i className="bi bi-calendar text-muted"></i>
                  </span>
                  <input
                    className="form-control"
                    type="date"
                    value={filters.fromDate}
                    onChange={(e) => setFilters({ ...filters, fromDate: e.target.value })}
                  />
                </div>
              </div>
              <div className="col-md-2">
                <label className="form-label">Age</label>
                <div className="input-group">
                  <span className="input-group-text bg-light">
                    <i className="bi bi-person text-muted"></i>
                  </span>
                  <input
                    className="form-control"
                    type="number"
                    min="1"
                    max="120"
                    placeholder="18+"
                    value={filters.age}
                    onChange={(e) => setFilters({ ...filters, age: e.target.value })}
                  />
                </div>
              </div>
              <div className="col-md-3 d-grid">
                <button className="btn btn-primary" onClick={load}>
                  <i className="bi bi-search me-2"></i>Search
                </button>
              </div>
              <div className="col-md-1">
                <button className="btn btn-outline-secondary w-100" onClick={() => { setFilters({ city: "", fromDate: "", age: "" }); load(); }}>
                  <i className="bi bi-arrow-counterclockwise"></i>
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
            <p className="mt-3 text-muted">Loading vaccination drives...</p>
          </div>
        ) : drives.length > 0 ? (
          <>
            <div className="d-flex justify-content-between align-items-center mb-4">
              <p className="text-muted mb-0">
                Showing <strong>{drives.length}</strong> drive{drives.length !== 1 ? 's' : ''}
              </p>
              <div className="btn-group">
                <button 
                  className={`btn btn-sm ${viewMode === 'grid' ? 'btn-primary' : 'btn-outline-primary'}`}
                  onClick={() => setViewMode('grid')}
                >
                  <i className="bi bi-grid-3x3-gap"></i>
                </button>
                <button 
                  className={`btn btn-sm ${viewMode === 'list' ? 'btn-primary' : 'btn-outline-primary'}`}
                  onClick={() => setViewMode('list')}
                >
                  <i className="bi bi-list-ul"></i>
                </button>
              </div>
            </div>
            
            <div className={viewMode === 'grid' ? 'row g-4' : 'd-flex flex-column gap-3'}>
              {drives.map((d, index) => (
                <div key={d.id} className={`${viewMode === 'grid' ? 'col-md-6 col-lg-4' : ''} fade-in stagger-${(index % 6) + 1}`}>
                  <div className={`drive-card h-100 ${viewMode === 'list' ? 'flex-row' : ''}`}>
                    {viewMode === 'grid' && (
                      <div className="card-header d-flex justify-content-between align-items-center">
                        <h5 className="mb-0" style={{fontSize: '1rem'}}>{d.name}</h5>
                        {!d.hasSlots ? (
                          <span className="badge bg-warning">No Slots</span>
                        ) : d.availableSlots > 0 ? (
                          <span className="badge bg-white text-primary">{d.availableSlots} left</span>
                        ) : (
                          <span className="badge bg-danger">Full</span>
                        )}
                      </div>
                    )}
                    <div className={`card-body ${viewMode === 'list' ? 'd-flex flex-row align-items-center gap-4' : ''}`}>
                      {viewMode === 'list' && (
                        <div className="text-center p-3 bg-primary bg-opacity-10 rounded">
                          <i className="bi bi-calendar-event display-6 text-primary"></i>
                        </div>
                      )}
                      <div className="flex-grow-1">
                        {viewMode === 'list' && (
                          <div className="d-flex justify-content-between align-items-start mb-2">
                            <h5 className="fw-bold mb-0">{d.name}</h5>
                            {!d.hasSlots ? (
                              <span className="badge bg-warning">No Slots</span>
                            ) : d.availableSlots > 0 ? (
                              <span className="badge bg-success">{d.availableSlots} slots left</span>
                            ) : (
                              <span className="badge bg-danger">Full</span>
                            )}
                          </div>
                        )}
                        <div className="row g-2">
                          <div className="col-6">
                            <div className="info-item">
                              <i className="bi bi-calendar-event"></i>
                              <span>{new Date(d.date).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })}</span>
                            </div>
                          </div>
                          <div className="col-6">
                            <div className="info-item">
                              <i className="bi bi-clock"></i>
                              <span>{d.startTime} - {d.endTime}</span>
                            </div>
                          </div>
                          <div className="col-6">
                            <div className="info-item">
                              <i className="bi bi-building"></i>
                              <span>{d.centerName}</span>
                            </div>
                          </div>
                          <div className="col-6">
                            <div className="info-item">
                              <i className="bi bi-person-badge"></i>
                              <span>Age: {d.minAge}-{d.maxAge}</span>
                            </div>
                          </div>
                        </div>
                        
                        {/* Progress Bar */}
                        <div className="mt-3">
                          <div className="d-flex justify-content-between mb-1">
                            <small className="text-muted">Capacity</small>
                            <small className="text-muted">{d.availableSlots}/{d.totalSlots}</small>
                          </div>
                          <div className="slots-progress">
                            <div 
                              className="progress-bar" 
                              style={{ width: `${((d.totalSlots - d.availableSlots) / d.totalSlots) * 100}%` }}
                            ></div>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="card-footer bg-white border-top-0 pt-0">
                      {!d.hasSlots ? (
                        <button className="btn btn-secondary w-100" disabled>
                          <i className="bi bi-x-circle me-2"></i>No Slots Available
                        </button>
                      ) : d.availableSlots > 0 ? (
                        <Link to={`/login?redirect=/drives?book=${d.id}`} className="btn btn-primary w-100">
                          <i className="bi bi-bookmark-plus me-2"></i>Book Now
                        </Link>
                      ) : (
                        <button className="btn btn-secondary w-100" disabled>
                          <i className="bi bi-x-circle me-2"></i>Fully Booked
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </>
        ) : (
          <div className="empty-state">
            <i className="bi bi-calendar-x"></i>
            <h5>No Drives Found</h5>
            <p>Try adjusting your search filters or check back later.</p>
            <button className="btn btn-primary" onClick={() => { setFilters({ city: "", fromDate: "", age: "" }); load(); }}>
              Clear Filters
            </button>
          </div>
        )}

        {/* How to Book */}
        <div className="mt-5">
          <div className="card border-0 shadow-sm">
            <div className="card-body py-4">
              <h4 className="fw-bold mb-4 text-center">How to Book Your Vaccination</h4>
              <div className="row g-4">
                <div className="col-md-4 text-center">
                  <div className="feature-card shadow-none">
                    <div className="icon-wrapper">
                      <i className="bi bi-person-plus"></i>
                    </div>
                    <h6 className="fw-bold">1. Create Account</h6>
                    <p className="small text-muted mb-0">Register for free to get started</p>
                  </div>
                </div>
                <div className="col-md-4 text-center">
                  <div className="feature-card shadow-none">
                    <div className="icon-wrapper">
                      <i className="bi bi-search"></i>
                    </div>
                    <h6 className="fw-bold">2. Find a Drive</h6>
                    <p className="small text-muted mb-0">Search by location and date</p>
                  </div>
                </div>
                <div className="col-md-4 text-center">
                  <div className="feature-card shadow-none">
                    <div className="icon-wrapper">
                      <i className="bi bi-check-circle"></i>
                    </div>
                    <h6 className="fw-bold">3. Book Slot</h6>
                    <p className="small text-muted mb-0">Confirm your appointment</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
