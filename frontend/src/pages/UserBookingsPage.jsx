import React, { useEffect, useState } from "react";
import api from "../api/client";

export default function UserBookingsPage() {
  const [bookings, setBookings] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [profile, setProfile] = useState(null);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState("");
  const [activeTab, setActiveTab] = useState("bookings");
  const [forms, setForms] = useState({
    slotId: ""
  });

  const loadData = async () => {
    setLoading(true);
    try {
      const [bookingsRes, notificationsRes, profileRes, drivesRes] = await Promise.all([
        api.get("/user/bookings"),
        api.get("/user/notifications"),
        api.get("/user/account"),
        api.get("/public/drives")
      ]);
      setBookings(bookingsRes.data || []);
      setNotifications(notificationsRes.data || []);
      setProfile(profileRes.data || null);
      
      // Map backend response to extract available slots
      const drivesPayload = drivesRes.data;
      const drives = Array.isArray(drivesPayload)
        ? drivesPayload
        : (drivesPayload?.drives || []);
      const slots = [];
      drives.forEach(drive => {
        if (drive.slots) {
          drive.slots.forEach(slot => {
            slots.push({
              ...slot,
              driveTitle: drive.title,
              driveDate: drive.driveDate,
              centerName: drive.center?.name
            });
          });
        }
      });
      setAvailableSlots(slots.filter(s => s.capacity > (s.bookedCount || 0)));
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

  const bookSlot = async (slotId) => {
    try {
      await api.post("/user/bookings", { slotId });
      setMsg("Booking request submitted successfully.");
      setForms({ ...forms, slotId: "" });
      await loadData();
      setActiveTab("bookings");
    } catch (error) {
      setMsg("Failed to book slot. Please try again.");
    }
  };

  const cancelBooking = async (id) => {
    if (!window.confirm("Are you sure you want to cancel this booking?")) return;
    try {
      await api.patch(`/user/bookings/${id}/cancel`);
      setMsg("Booking cancelled successfully.");
      await loadData();
    } catch (error) {
      setMsg("Failed to cancel booking.");
    }
  };

  const getStatusBadge = (status) => {
    const badges = {
      PENDING: "bg-warning text-dark",
      APPROVED: "bg-success",
      REJECTED: "bg-danger",
      CANCELLED: "bg-secondary"
    };
    return <span className={`badge ${badges[status] || "bg-secondary"}`}>{status}</span>;
  };

  const pendingBookings = bookings.filter(b => b.status === "PENDING");
  const approvedBookings = bookings.filter(b => b.status === "APPROVED");
  
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
      {/* Page Header */}
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

      {/* Profile Card */}
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

      {/* Stats Row */}
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
          <div className="stats-card bg-success">
            <div className="stat-number">{approvedBookings.length}</div>
            <div className="stat-label">Approved</div>
          </div>
        </div>
        <div className="col-6 col-lg-3">
          <div className="stats-card bg-secondary">
            <div className="stat-number">{notifications.length}</div>
            <div className="stat-label">Notifications</div>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
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
          </button>
        </li>
      </ul>

      {/* My Bookings Tab */}
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
                    {bookings.map((b) => (
                      <tr key={b.id}>
                        <td><strong>#{b.id}</strong></td>
                        <td>{b.slot?.drive?.title}</td>
                        <td><small>{new Date(b.slot?.startTime).toLocaleString()}</small></td>
                        <td><small>{b.slot?.drive?.center?.name}</small></td>
                        <td>{getStatusBadge(b.status)}</td>
                        <td>
                          <div className="btn-group btn-group-sm">
                            {b.status === "PENDING" && (
                              <>
                                <button className="btn btn-outline-danger" title="Cancel" onClick={() => cancelBooking(b.id)}>
                                  <i className="bi bi-x-circle"></i>
                                </button>
                              </>
                            )}
                            {b.status === "APPROVED" && (
                              <>
                                <button className="btn btn-outline-warning" title="Cancel" onClick={() => cancelBooking(b.id)}>
                                  <i className="bi bi-x-circle"></i> Cancel
                                </button>
                              </>
                            )}
                            {["REJECTED", "CANCELLED"].includes(b.status) && (
                              <button className="btn btn-outline-primary" title="Book Again" onClick={() => setActiveTab("slots")}>
                                <i className="bi bi-arrow-repeat"></i>
                              </button>
                            )}
                          </div>
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

      {/* Book New Slot Tab */}
      {activeTab === "book" && (
        <div className="row">
          <div className="col-lg-6">
            <div className="card">
              <div className="card-header">
                <i className="bi bi-plus-circle me-2"></i>Book a Slot
              </div>
              <div className="card-body">
                <form onSubmit={bookSlot} className="d-grid gap-3">
                  <div>
                    <label className="form-label">Enter Slot ID</label>
                    <div className="input-group">
                      <span className="input-group-text"><i className="bi bi-hash"></i></span>
                      <input 
                        className="form-control" 
                        placeholder="e.g., 1" 
                        value={forms.slotId}
                        onChange={(e) => setForms({ ...forms, slotId: e.target.value })}
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
          <div className="col-lg-6">
            <div className="card bg-info bg-opacity-10 border-0">
              <div className="card-body">
                <h5><i className="bi bi-info-circle me-2"></i>How to Book</h5>
                <ol className="mb-0">
                  <li className="mb-2">Go to the <strong>Available Slots</strong> tab</li>
                  <li className="mb-2">Find a slot that works for you</li>
                  <li className="mb-2">Copy the <strong>Slot ID</strong></li>
                  <li className="mb-2">Enter the Slot ID above and submit</li>
                  <li>Wait for admin approval - you'll be notified!</li>
                </ol>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Available Slots Tab */}
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
                    {availableSlots.map((s) => (
                      <tr key={s.id}>
                        <td><strong>#{s.id}</strong></td>
                        <td>{s.driveTitle || s.drive?.title}</td>
                        <td><small>{new Date(s.startTime).toLocaleString()}</small></td>
                        <td><small>{s.centerName || s.drive?.center?.name}</small></td>
                        <td>{s.capacity}</td>
                        <td>
                          <button 
                            className="btn btn-primary btn-sm"
                            onClick={() => bookSlot(s.id)}
                          >
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

      {/* Notifications Tab */}
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
                {notifications.slice(0, 20).map((n) => (
                  <li key={n.id} className="list-group-item d-flex justify-content-between align-items-start p-3">
                    <div className="d-flex gap-3">
                      <div className={`bg-${n.channel === "EMAIL" ? "primary" : "success"} bg-opacity-10 rounded-circle p-2`}>
                        <i className={`bi ${n.channel === "EMAIL" ? "bi-envelope" : "bi-phone"} text-${n.channel === "EMAIL" ? "primary" : "success"}`}></i>
                      </div>
                      <div>
                        <div className="fw-semibold">{n.channel === "EMAIL" ? "Email Sent" : "SMS Sent"}</div>
                        <div className="text-muted small">{n.message}</div>
                      </div>
                    </div>
                    <small className="text-muted">{new Date(n.createdAt).toLocaleString()}</small>
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
