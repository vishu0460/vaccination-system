import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Helmet } from "react-helmet-async";
import { Button, Modal } from "react-bootstrap";
import { publicAPI, unwrapApiData, userAPI } from "../api/client";
import { isAuthenticated } from "../utils/auth";

export default function DrivesPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [drives, setDrives] = useState([]);
  const [userProfile, setUserProfile] = useState(null);
  const [summary, setSummary] = useState({ totalCenters: 0, activeDrives: 0, availableSlots: 0 });
  const [filters, setFilters] = useState({ city: "", fromDate: "", age: "" });
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState("grid");
  const [error, setError] = useState("");
  const [bookingDrive, setBookingDrive] = useState(null);
  const [bookingSlots, setBookingSlots] = useState([]);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [bookingSubmittingId, setBookingSubmittingId] = useState(null);
  const [bookingMessage, setBookingMessage] = useState("");

  const formatAppointmentTime = (value) => {
    if (!value) {
      return "";
    }

    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime())
      ? ""
      : parsed.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
  };

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams();
      if (filters.city.trim()) params.set("city", filters.city.trim());
      if (filters.fromDate) params.set("fromDate", filters.fromDate);
      if (filters.age) params.set("age", filters.age);

      const [drivesRes, summaryRes] = await Promise.all([
        publicAPI.getDrives(Object.fromEntries(params)),
        publicAPI.getSummary()
      ]);
      const drivesPayload = unwrapApiData(drivesRes) || {};
      const drivesData = Array.isArray(drivesPayload)
        ? drivesPayload
        : (drivesPayload.drives || []);
      const mappedDrives = drivesData.map((drive) => ({
        ...drive,
        name: drive.title,
        date: drive.driveDate,
        centerName: drive.center?.name || drive.centerName,
        hasSlots: (drive.availableSlots ?? drive.totalSlots ?? 0) > 0,
        availableSlots: drive.availableSlots ?? drive.totalSlots ?? 0,
        totalSlots: drive.totalSlots || 0,
        startTime: drive.startTime || "N/A",
        endTime: drive.endTime || "N/A"
      }));
      setDrives(mappedDrives);
      const summaryPayload = unwrapApiData(summaryRes) || {};
      setSummary({
        totalCenters: summaryPayload.totalCenters || summaryPayload.centersCount || 0,
        activeDrives: summaryPayload.activeDrives || summaryPayload.drivesCount || 0,
        availableSlots: summaryPayload.availableSlots || 0
      });
    } catch (requestError) {
      console.error("Error fetching drives:", requestError);
      setError("Unable to load vaccination drives right now. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    const loadProfile = async () => {
      if (!isAuthenticated()) {
        setUserProfile(null);
        return;
      }

      try {
        const response = await userAPI.getProfile();
        setUserProfile(unwrapApiData(response));
      } catch (requestError) {
        setUserProfile(null);
      }
    };

    loadProfile();
  }, []);

  useEffect(() => {
    const handleDataUpdated = () => {
      load();
    };

    window.addEventListener("vaxzone:data-updated", handleDataUpdated);
    return () => window.removeEventListener("vaxzone:data-updated", handleDataUpdated);
  }, [filters.city, filters.fromDate, filters.age]);

  useEffect(() => {
    const bookId = searchParams.get("book");
    if (!bookId || drives.length === 0) {
      return;
    }

    const drive = drives.find((item) => item.id === Number.parseInt(bookId, 10));
    if (!drive) {
      return;
    }

    if (!isAuthenticated()) {
      navigate(`/login?redirect=${encodeURIComponent(`/drives?book=${drive.id}`)}`, { replace: true });
      return;
    }

    openBookingFlow(drive);
  }, [searchParams, drives, navigate]);

  const normalizeSlot = (slot, drive) => ({
    ...slot,
    driveId: drive.id,
    driveTitle: drive.title || drive.name,
    centerName: drive.center?.name || drive.centerName,
    availableCapacity: Math.max(0, (slot.capacity || 0) - (slot.bookedCount || 0))
  });

  const openBookingFlow = async (drive) => {
    setBookingDrive(drive);
    setBookingMessage("");
    setBookingLoading(true);

    try {
      const response = await publicAPI.getDriveSlots(drive.id);
      const slots = (unwrapApiData(response) || [])
        .map((slot) => normalizeSlot(slot, drive))
        .filter((slot) => slot.availableCapacity > 0);

      setBookingSlots(slots);
    } catch (requestError) {
      console.error("Failed to load slots for booking:", requestError);
      setBookingSlots([]);
      setBookingMessage("Unable to load slots for this drive right now.");
    } finally {
      setBookingLoading(false);
    }
  };

  const closeBookingModal = () => {
    setBookingDrive(null);
    setBookingSlots([]);
    setBookingLoading(false);
    setBookingSubmittingId(null);
    setBookingMessage("");

    if (searchParams.get("book")) {
      const nextParams = new URLSearchParams(searchParams);
      nextParams.delete("book");
      setSearchParams(nextParams, { replace: true });
    }
  };

  const submitBooking = async (slot) => {
    if (!slot?.id) {
      setBookingMessage("Select a slot before booking.");
      return;
    }

    if (!userProfile?.id) {
      setBookingMessage("Please log in again before booking.");
      return;
    }

    setBookingSubmittingId(slot.id);
    setBookingMessage("");

    try {
      const response = await userAPI.bookSlot({
        userId: userProfile.id,
        slotId: slot.id,
        driveId: slot.driveId
      });
      const booking = unwrapApiData(response);
      const assignedTime = formatAppointmentTime(booking?.assignedTime);
      setBookingMessage(
        assignedTime
          ? `Booking created successfully. Your appointment time: ${assignedTime}`
          : "Booking created successfully."
      );
      window.dispatchEvent(new CustomEvent("vaxzone:data-updated"));
      await load();
      setTimeout(() => {
        closeBookingModal();
        navigate("/user/bookings");
      }, 500);
    } catch (requestError) {
      console.error("Booking failed:", requestError);
      setBookingMessage(requestError.response?.data?.message || "Failed to book this slot.");
    } finally {
      setBookingSubmittingId(null);
    }
  };

  const eventJsonLd = useMemo(() => ({
    "@context": "https://schema.org",
    "@type": "ItemList",
    itemListElement: drives.map((drive, index) => ({
      "@type": "Event",
      position: index + 1,
      name: drive.title || drive.name,
      startDate: drive.driveDate || drive.date,
      location: {
        "@type": "Place",
        name: drive.center?.name || drive.centerName,
        address: drive.center?.address
      }
    }))
  }), [drives]);

  const renderBookButton = (drive) => {
    if (!drive.hasSlots || drive.availableSlots <= 0) {
      return (
        <button className="btn btn-secondary w-100" disabled>
          <i className="bi bi-x-circle me-2"></i>No Slots Available
        </button>
      );
    }

    return (
      <button className="btn btn-primary w-100" onClick={() => openBookingFlow(drive)}>
        <i className="bi bi-bookmark-plus me-2"></i>Book Now
      </button>
    );
  };

  return (
    <>
      <Helmet>
        <title>Vaccination Drives - VaxZone</title>
        <meta name="description" content="Find and book upcoming vaccination drives near you. Browse by city, date, and availability." />
        <meta property="og:title" content="Vaccination Drives - VaxZone" />
        <meta property="og:description" content="Find and book upcoming vaccination drives near you." />
        <script type="application/ld+json">{JSON.stringify(eventJsonLd)}</script>
      </Helmet>

      <section className="page-header">
        <div className="container">
          <div className="row align-items-center">
            <div className="col-lg-8">
              <h1 className="mb-2">Vaccination Drives</h1>
              <p className="mb-0 opacity-75">Find and book your vaccination slot at a drive near you</p>
            </div>
            <div className="col-lg-4 text-center text-lg-end mt-3 mt-lg-0">
              <i className="bi bi-calendar-event display-1" style={{ opacity: 0.3 }}></i>
            </div>
          </div>
        </div>
      </section>

      <div className="container pb-5">
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
                    onChange={(event) => setFilters({ ...filters, city: event.target.value })}
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
                    onChange={(event) => setFilters({ ...filters, fromDate: event.target.value })}
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
                    onChange={(event) => setFilters({ ...filters, age: event.target.value })}
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

        {loading ? (
          <div className="text-center py-5">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-3 text-muted">Loading vaccination drives...</p>
          </div>
        ) : error ? (
          <div className="empty-state">
            <i className="bi bi-wifi-off"></i>
            <h5>Unable to load drives</h5>
            <p>{error}</p>
            <button className="btn btn-primary" onClick={load}>Retry</button>
          </div>
        ) : drives.length > 0 ? (
          <>
            <div className="d-flex justify-content-between align-items-center mb-4">
              <p className="text-muted mb-0">
                Showing <strong>{drives.length}</strong> drive{drives.length !== 1 ? "s" : ""}
              </p>
              <div className="btn-group">
                <button
                  className={`btn btn-sm ${viewMode === "grid" ? "btn-primary" : "btn-outline-primary"}`}
                  onClick={() => setViewMode("grid")}
                >
                  <i className="bi bi-grid-3x3-gap"></i>
                </button>
                <button
                  className={`btn btn-sm ${viewMode === "list" ? "btn-primary" : "btn-outline-primary"}`}
                  onClick={() => setViewMode("list")}
                >
                  <i className="bi bi-list-ul"></i>
                </button>
              </div>
            </div>

            <div className={viewMode === "grid" ? "row g-4" : "d-flex flex-column gap-3"}>
              {drives.map((drive, index) => (
                <div key={drive.id} className={`${viewMode === "grid" ? "col-md-6 col-lg-4" : ""} fade-in stagger-${(index % 6) + 1}`}>
                  <div className={`drive-card h-100 ${viewMode === "list" ? "flex-row" : ""}`}>
                    {viewMode === "grid" && (
                      <div className="card-header d-flex justify-content-between align-items-center">
                        <h5 className="mb-0" style={{ fontSize: "1rem" }}>{drive.name}</h5>
                        {!drive.hasSlots ? (
                          <span className="badge bg-warning">No Slots</span>
                        ) : drive.availableSlots > 0 ? (
                          <span className="badge bg-white text-primary">{drive.availableSlots} left</span>
                        ) : (
                          <span className="badge bg-danger">Full</span>
                        )}
                      </div>
                    )}
                    <div className={`card-body ${viewMode === "list" ? "d-flex flex-row align-items-center gap-4" : ""}`}>
                      {viewMode === "list" && (
                        <div className="text-center p-3 bg-primary bg-opacity-10 rounded">
                          <i className="bi bi-calendar-event display-6 text-primary"></i>
                        </div>
                      )}
                      <div className="flex-grow-1">
                        {viewMode === "list" && (
                          <div className="d-flex justify-content-between align-items-start mb-2">
                            <h5 className="fw-bold mb-0">{drive.name}</h5>
                            {!drive.hasSlots ? (
                              <span className="badge bg-warning">No Slots</span>
                            ) : drive.availableSlots > 0 ? (
                              <span className="badge bg-success">{drive.availableSlots} slots left</span>
                            ) : (
                              <span className="badge bg-danger">Full</span>
                            )}
                          </div>
                        )}
                        <div className="row g-2">
                          <div className="col-6">
                            <div className="info-item">
                              <i className="bi bi-calendar-event"></i>
                              <span>{new Date(drive.date).toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" })}</span>
                            </div>
                          </div>
                          <div className="col-6">
                            <div className="info-item">
                              <i className="bi bi-clock"></i>
                              <span>{drive.startTime} - {drive.endTime}</span>
                            </div>
                          </div>
                          <div className="col-6">
                          <div className="info-item">
                              <i className="bi bi-building"></i>
                              <span>{drive.centerName}</span>
                            </div>
                          </div>
                          <div className="col-6">
                            <div className="info-item">
                              <i className="bi bi-broadcast"></i>
                              <span>{drive.status || "UPCOMING"}</span>
                            </div>
                          </div>
                          <div className="col-6">
                            <div className="info-item">
                              <i className="bi bi-person-badge"></i>
                              <span>Age: {drive.minAge}-{drive.maxAge}</span>
                            </div>
                          </div>
                        </div>

                        <div className="mt-3">
                          <div className="d-flex justify-content-between mb-1">
                            <small className="text-muted">Capacity</small>
                            <small className="text-muted">{drive.availableSlots}/{drive.totalSlots}</small>
                          </div>
                          <div className="slots-progress">
                            <div
                              className="progress-bar"
                              style={{ width: `${drive.totalSlots > 0 ? ((drive.totalSlots - drive.availableSlots) / drive.totalSlots) * 100 : 0}%` }}
                            ></div>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="card-footer bg-white border-top-0 pt-0">
                      {renderBookButton(drive)}
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

      <Modal show={Boolean(bookingDrive)} onHide={closeBookingModal} centered size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Book Slot{bookingDrive ? ` at ${bookingDrive.centerName}` : ""}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {bookingMessage ? (
            <div className={`alert ${bookingMessage.toLowerCase().includes("successfully") ? "alert-success" : "alert-danger"}`}>
              {bookingMessage}
            </div>
          ) : null}
          {bookingLoading ? (
            <div className="text-center py-4">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Loading...</span>
              </div>
            </div>
          ) : bookingSlots.length === 0 ? (
            <div className="text-center py-4 text-muted">
              No bookable slots are available for this drive right now.
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table align-middle">
                <thead>
                  <tr>
                    <th>Slot ID</th>
                    <th>Start</th>
                    <th>End</th>
                    <th>Available</th>
                    <th className="text-end">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {bookingSlots.map((slot) => (
                    <tr key={slot.id}>
                      <td>#{slot.id}</td>
                      <td>{slot.dateTime ? new Date(slot.dateTime).toLocaleString() : "N/A"}</td>
                      <td>{slot.endTime || "N/A"}</td>
                      <td>{slot.availableCapacity}</td>
                      <td className="text-end">
                        <Button
                          onClick={() => submitBooking(slot)}
                          disabled={bookingSubmittingId === slot.id}
                        >
                          {bookingSubmittingId === slot.id ? "Booking..." : "Book"}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Modal.Body>
      </Modal>
    </>
  );
}
