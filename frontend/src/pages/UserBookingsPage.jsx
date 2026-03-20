import React, { useEffect, useState } from "react";
import api, { certificateAPI, publicAPI, unwrapApiData } from "../api/client";

export default function UserBookingsPage() {
  const [bookings, setBookings] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [profile, setProfile] = useState(null);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState("");
  const [activeTab, setActiveTab] = useState("bookings");
  const [forms, setForms] = useState({ slotId: "" });

  const loadData = async () => {
    setLoading(true);
    try {
      const [bookingsRes, notificationsRes, profileRes, drivesRes] = await Promise.all([
        api.get("/user/bookings"),
        api.get("/user/notifications"),
        api.get("/profile"),
        publicAPI.getDrives(),
        
      ]);
      const certificatesRes = await certificateAPI.getMyCertificates();

      setBookings(unwrapApiData(bookingsRes) || []);
      setNotifications(unwrapApiData(notificationsRes) || []);
      setProfile(unwrapApiData(profileRes) || null);
      setCertificates(unwrapApiData(certificatesRes) || certificatesRes.data || []);

      const drivesPayload = unwrapApiData(drivesRes) || {};
      console.log("User drives response:", drivesPayload);
      const drives = Array.isArray(drivesPayload) ? drivesPayload : (drivesPayload.drives || []);
      const slotResponses = await Promise.all(drives.map((drive) => publicAPI.getDriveSlots(drive.id)));
      const slots = [];

      slotResponses.forEach((slotResponse, index) => {
        const drive = drives[index];
        (unwrapApiData(slotResponse) || []).forEach((slot) => {
          slots.push({
            ...slot,
            driveId: drive.id,
            driveTitle: drive.title,
            driveDate: drive.driveDate,
            centerName: drive.center?.name || drive.centerName
          });
        });
      });

      setAvailableSlots(slots.filter((slot) => slot.capacity > (slot.bookedCount || 0)));
    } catch (error) {
      console.error("Error loading data:", error);
      setMsg("Unable to load your data. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const buildBookingPayload = (slotId) => {
    const selectedSlot = availableSlots.find((slot) => slot.id === slotId);
    return {
      slotId,
      driveId: selectedSlot?.driveId
    };
  };

  const bookSlot = async (slotId) => {
    try {
      const payload = buildBookingPayload(slotId);
      console.log("Booking payload:", payload);
      await api.post("/user/bookings", payload);
      setMsg("Booking request submitted successfully.");
      setForms({ slotId: "" });
      await loadData();
      setActiveTab("bookings");
    } catch (error) {
      setMsg(error.response?.data?.message || "Failed to book slot. Please try again.");
    }
  };

  const cancelBooking = async (id) => {
    if (!window.confirm("Are you sure you want to cancel this booking?")) return;
    try {
      await api.patch(`/user/bookings/${id}/cancel`);
      setMsg("Booking cancelled successfully.");
      await loadData();
    } catch (error) {
      setMsg(error.response?.data?.message || "Failed to cancel booking.");
    }
  };

  const getStatusBadge = (status) => {
    const badges = {
      PENDING: "bg-warning text-dark",
      CONFIRMED: "bg-info text-dark",
      COMPLETED: "bg-success",
      CANCELLED: "bg-danger"
    };
    return <span className={`badge ${badges[status] || "bg-secondary"}`}>{status}</span>;
  };

  const pendingBookings = bookings.filter((booking) => booking.status === "PENDING");
  const confirmedBookings = bookings.filter((booking) => booking.status === "CONFIRMED");
  const completedBookings = bookings.filter((booking) => booking.status === "COMPLETED");

  const getCertificateForBooking = (bookingId) => certificates.find((certificate) => certificate.bookingId === bookingId);

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ minHeight: "50vh" }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="container py-4">
      <div className="page-header rounded-3 mb-4 p-4">
        <div className="d-flex justify-content-between align-items-center flex-wrap gap-3">
          <div>
            <h2 className="mb-1"><i className="bi bi-calendar-check me-2"></i>My Bookings</h2>
            <p className="mb-0 opacity-75">Manage your vaccination bookings and view notifications</p>
          </div>
          <button className="btn btn-light" onClick={loadData}>
            <i className="bi bi-arrow-clockwise me-2"></i>Refresh
          </button>
        </div>
      </div>

      {msg && (
        <div className={`alert ${msg.toLowerCase().includes("failed") || msg.toLowerCase().includes("unable") ? "alert-danger" : "alert-success"} alert-dismissible fade show`} role="alert">
          {msg}
          <button type="button" className="btn-close" onClick={() => setMsg("")}></button>
        </div>
      )}

      {profile && (
        <div className="card mb-4">
          <div className="card-body d-flex align-items-center gap-3">
            <div className="bg-primary bg-opacity-10 rounded-circle p-3">
              <i className="bi bi-person-fill fs-4 text-primary"></i>
            </div>
            <div className="flex-grow-1">
              <h5 className="mb-1">{profile.fullName}</h5>
              <p className="mb-0 text-muted">
                <i className="bi bi-envelope me-2"></i>{profile.email}
                <span className="mx-2">|</span>
                <i className="bi bi-person-badge me-2"></i>Age: {profile.age}
              </p>
            </div>
            <div>
              {profile.emailVerified ? (
                <span className="badge bg-success"><i className="bi bi-check-circle me-1"></i>Verified</span>
              ) : (
                <span className="badge bg-warning text-dark"><i className="bi bi-exclamation-circle me-1"></i>Unverified</span>
              )}
            </div>
          </div>
        </div>
      )}

      <div className="row g-3 mb-4">
        <div className="col-6 col-lg-3">
          <div className="stats-card">
            <div className="stat-number">{bookings.length}</div>
            <div className="stat-label">Total</div>
          </div>
        </div>
        <div className="col-6 col-lg-3">
          <div className="stats-card bg-warning">
            <div className="stat-number">{pendingBookings.length}</div>
            <div className="stat-label">Pending</div>
          </div>
        </div>
        <div className="col-6 col-lg-3">
          <div className="stats-card bg-info">
            <div className="stat-number">{confirmedBookings.length}</div>
            <div className="stat-label">Confirmed</div>
          </div>
        </div>
        <div className="col-6 col-lg-3">
          <div className="stats-card bg-success">
            <div className="stat-number">{completedBookings.length}</div>
            <div className="stat-label">Completed</div>
          </div>
        </div>
      </div>

      <ul className="nav nav-tabs mb-4">
        <li className="nav-item">
          <button className={`nav-link ${activeTab === "bookings" ? "active" : ""}`} onClick={() => setActiveTab("bookings")}>
            <i className="bi bi-calendar-check me-2"></i>My Bookings
          </button>
        </li>
        <li className="nav-item">
          <button className={`nav-link ${activeTab === "book" ? "active" : ""}`} onClick={() => setActiveTab("book")}>
            <i className="bi bi-plus-circle me-2"></i>Book New Slot
          </button>
        </li>
        <li className="nav-item">
          <button className={`nav-link ${activeTab === "slots" ? "active" : ""}`} onClick={() => setActiveTab("slots")}>
            <i className="bi bi-clock me-2"></i>Available Slots
            <span className="badge bg-primary ms-2">{availableSlots.length}</span>
          </button>
        </li>
        <li className="nav-item">
          <button className={`nav-link ${activeTab === "notifications" ? "active" : ""}`} onClick={() => setActiveTab("notifications")}>
            <i className="bi bi-bell me-2"></i>Notifications
            <span className="badge bg-secondary ms-2">{notifications.length}</span>
          </button>
        </li>
      </ul>

      {activeTab === "bookings" && (
        <div className="card">
          <div className="card-header d-flex justify-content-between align-items-center">
            <span><i className="bi bi-calendar-check me-2"></i>All Bookings</span>
            <span className="badge bg-primary">{bookings.length} Total</span>
          </div>
          <div className="card-body p-0">
            {bookings.length === 0 ? (
              <div className="empty-state">
                <i className="bi bi-calendar-x"></i>
                <h5>No Bookings Yet</h5>
                <p>You haven't made any bookings yet. Browse available slots to book your vaccination.</p>
                <button className="btn btn-primary" onClick={() => setActiveTab("slots")}>
                  <i className="bi bi-search me-2"></i>Find Available Slots
                </button>
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover mb-0">
                  <thead>
                    <tr>
                      <th><i className="bi bi-hash"></i> ID</th>
                      <th><i className="bi bi-calendar-event me-1"></i>Drive</th>
                      <th><i className="bi bi-clock me-1"></i>Slot Time</th>
                      <th><i className="bi bi-building me-1"></i>Center</th>
                      <th><i className="bi bi-info-circle me-1"></i>Status</th>
                      <th><i className="bi bi-gear me-1"></i>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bookings.map((booking) => (
                      <tr key={booking.id}>
                        <td><strong>#{booking.id}</strong></td>
                        <td>{booking.driveName || "-"}</td>
                        <td><small>{booking.slotTime ? new Date(booking.slotTime).toLocaleString() : "-"}</small></td>
                        <td><small>{booking.centerName || "-"}</small></td>
                        <td>{getStatusBadge(booking.status)}</td>
                        <td>
                          {(booking.status === "PENDING" || booking.status === "CONFIRMED") && (
                            <button className="btn btn-outline-danger btn-sm" title="Cancel" onClick={() => cancelBooking(booking.id)}>
                              <i className="bi bi-x-circle"></i>
                            </button>
                          )}
                          {booking.status === "COMPLETED" && getCertificateForBooking(booking.id) && (
                            <button className="btn btn-outline-success btn-sm" title="Download Certificate" onClick={() => window.location.href = `/certificates`}>
                              <i className="bi bi-download"></i>
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === "book" && (
        <div className="row">
          <div className="col-lg-6">
            <div className="card">
              <div className="card-header">
                <i className="bi bi-plus-circle me-2"></i>Book a Slot
              </div>
              <div className="card-body">
                <form
                  onSubmit={(event) => {
                    event.preventDefault();
                    if (forms.slotId) {
                      bookSlot(Number(forms.slotId));
                    }
                  }}
                  className="d-grid gap-3"
                >
                  <div>
                    <label className="form-label">Enter Slot ID</label>
                    <div className="input-group">
                      <span className="input-group-text"><i className="bi bi-hash"></i></span>
                      <input
                        className="form-control"
                        placeholder="e.g., 1"
                        value={forms.slotId}
                        onChange={(event) => setForms({ slotId: event.target.value })}
                        required
                      />
                    </div>
                    <small className="text-muted">Enter the Slot ID from the Available Slots tab</small>
                  </div>
                  <button type="submit" className="btn btn-primary mt-2">
                    <i className="bi bi-check-lg me-2"></i>Submit Booking Request
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}

      {activeTab === "slots" && (
        <div className="card">
          <div className="card-header d-flex justify-content-between align-items-center">
            <span><i className="bi bi-clock me-2"></i>Available Slots</span>
            <span className="badge bg-success">{availableSlots.length} Available</span>
          </div>
          <div className="card-body p-0">
            {availableSlots.length === 0 ? (
              <div className="empty-state">
                <i className="bi bi-calendar-x"></i>
                <h5>No Slots Available</h5>
                <p>There are no available slots at the moment. Please check back later.</p>
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table table-hover mb-0">
                  <thead>
                    <tr>
                      <th><i className="bi bi-hash"></i> ID</th>
                      <th><i className="bi bi-calendar-event me-1"></i>Drive</th>
                      <th><i className="bi bi-clock me-1"></i>Time</th>
                      <th><i className="bi bi-building me-1"></i>Center</th>
                      <th><i className="bi bi-people me-1"></i>Capacity</th>
                      <th><i className="bi bi-gear me-1"></i>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {availableSlots.map((slot) => (
                      <tr key={slot.id}>
                        <td><strong>#{slot.id}</strong></td>
                        <td>{slot.driveTitle || slot.drive?.title}</td>
                        <td><small>{slot.dateTime ? new Date(slot.dateTime).toLocaleString() : "-"}</small></td>
                        <td><small>{slot.centerName || slot.drive?.center?.name}</small></td>
                        <td>{(slot.capacity || 0) - (slot.bookedCount || 0)} / {slot.capacity}</td>
                        <td>
                          <button className="btn btn-primary btn-sm" onClick={() => bookSlot(slot.id)}>
                            <i className="bi bi-bookmark-plus me-1"></i>Book
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {activeTab === "notifications" && (
        <div className="card">
          <div className="card-header d-flex justify-content-between align-items-center">
            <span><i className="bi bi-bell me-2"></i>Notification Inbox</span>
            <span className="badge bg-primary">{notifications.length} Total</span>
          </div>
          <div className="card-body p-0">
            {notifications.length === 0 ? (
              <div className="empty-state">
                <i className="bi bi-bell-slash"></i>
                <h5>No Notifications</h5>
                <p>You don't have any notifications yet.</p>
              </div>
            ) : (
              <ul className="list-group list-group-flush">
                {notifications.slice(0, 20).map((notification) => (
                  <li key={notification.id} className="list-group-item d-flex justify-content-between align-items-start p-3">
                    <div>
                      <div className="fw-semibold">{notification.channel === "EMAIL" ? "Email Sent" : "SMS Sent"}</div>
                      <div className="text-muted small">{notification.message}</div>
                    </div>
                    <small className="text-muted">{notification.createdAt ? new Date(notification.createdAt).toLocaleString() : ""}</small>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
