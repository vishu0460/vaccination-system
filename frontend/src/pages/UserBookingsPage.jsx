import React, { useCallback, useEffect, useRef, useState } from "react";
import { jsPDF } from "jspdf";
import api, { certificateAPI, newsAPI, publicAPI, unwrapApiData } from "../api/client";
import ModalPopup from "../components/ModalPopup";
import Seo from "../components/Seo";
import useCurrentTime from "../hooks/useCurrentTime";
import { getCountdownLabel, getRealtimeStatus, getStatusBadgeClass, isAtCapacity, isSlotBookable } from "../utils/realtimeStatus";
import { broadcastDataUpdated, subscribeToDataUpdates } from "../utils/dataSync";
import { usePublicCatalog } from "../context/PublicCatalogContext";

const REPLY_NOTIFICATION_TYPES = new Set(["CONTACT_REPLY", "FEEDBACK_REPLY"]);

const getSlotStartDateTime = (slot) =>
  slot?.startDateTime || slot?.startDate || slot?.dateTime || slot?.startTime || "";

const getSlotEndDateTime = (slot) =>
  slot?.endDateTime || slot?.endDate || slot?.endTime || "";

const getNotificationCopy = (notification) => {
  if (notification?.type === "NEWS") {
    return {
      message: `Announcement: ${notification.message || "No details available"}`,
      followUp: "Stay informed by checking the latest announcements."
    };
  }

  if (REPLY_NOTIFICATION_TYPES.has(notification?.type)) {
    return {
      message: `Your message: ${notification.message || "No message"}`,
      followUp: `Reply: ${notification.reply || "No reply yet"}`
    };
  }

  return {
    message: notification?.message || "No details available",
    followUp: notification?.scheduledTime
      ? `Scheduled for ${new Date(notification.scheduledTime).toLocaleString()}`
      : (notification?.deliveryStatus ? `Delivery status: ${notification.deliveryStatus}` : "Notification delivered.")
  };
};

export default function UserBookingsPage() {
  const { drives: publicDrives, refreshCatalog } = usePublicCatalog();
  const [bookings, setBookings] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [profile, setProfile] = useState(null);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState("");
  const [activeTab, setActiveTab] = useState("bookings");
  const [forms, setForms] = useState({ slotId: "" });
  const [bookingToCancel, setBookingToCancel] = useState(null);
  const [waitlistEntries, setWaitlistEntries] = useState([]);
  const [waitlistLoadingId, setWaitlistLoadingId] = useState(null);
  const [slotCityFilter, setSlotCityFilter] = useState("");
  const dataRequestInFlightRef = useRef(false);
  const slotsRequestInFlightRef = useRef(false);
  const now = useCurrentTime(1000);

  const formatAppointmentTime = (value) => {
    if (!value) {
      return "-";
    }

    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime())
      ? "-"
      : parsed.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
  };

  const loadData = useCallback(async (options = {}) => {
    const { silent = false } = options;
    if (dataRequestInFlightRef.current) {
      return;
    }
    dataRequestInFlightRef.current = true;
    if (!silent) {
      setLoading(true);
    }
    try {
      const [bookingsRes, notificationsRes, profileRes, newsRes, certificatesRes, waitlistRes] = await Promise.all([
        api.get("/user/bookings"),
        api.get("/user/notifications"),
        api.get("/profile"),
        newsAPI.getAllNews(0, 10),
        certificateAPI.getMyCertificates(),
        api.get("/user/waitlist").catch(() => ({ data: { data: [] } }))
      ]);

      setBookings(unwrapApiData(bookingsRes) || []);
      setProfile(unwrapApiData(profileRes) || null);
      setCertificates(unwrapApiData(certificatesRes) || certificatesRes.data || []);
      setWaitlistEntries(unwrapApiData(waitlistRes) || []);

      const baseNotifications = unwrapApiData(notificationsRes) || [];
      const latestNews = unwrapApiData(newsRes) || [];
      const newsNotifications = (Array.isArray(latestNews) ? latestNews : []).map((item) => ({
        id: `news-${item.id}`,
        title: `New update: ${item.title}`,
        type: "NEWS",
        message: item.content,
        reply: null,
        createdAt: item.createdAt || item.publishedAt || item.updatedAt,
        read: true
      }));

      const mergedNotifications = [...newsNotifications, ...baseNotifications]
        .sort((left, right) => {
          const leftTime = left?.createdAt ? new Date(left.createdAt).getTime() : 0;
          const rightTime = right?.createdAt ? new Date(right.createdAt).getTime() : 0;
          return rightTime - leftTime;
        });
      setNotifications(mergedNotifications);

    } catch (error) {
      setMsg("Unable to load your data. Please try again.");
    } finally {
      dataRequestInFlightRef.current = false;
      if (!silent) {
        setLoading(false);
      }
    }
  }, []);

  const loadAvailableSlots = useCallback(async () => {
    if (slotsRequestInFlightRef.current) {
      return;
    }
    slotsRequestInFlightRef.current = true;
    try {
      const slotResponses = await Promise.all(publicDrives.map((drive) => publicAPI.getDriveSlots(drive.id)));
      const slots = [];

      slotResponses.forEach((slotResponse, index) => {
        const drive = publicDrives[index];
        (unwrapApiData(slotResponse) || []).forEach((slot) => {
          const startDateTime = getSlotStartDateTime(slot);
          const endDateTime = getSlotEndDateTime(slot);
          slots.push({
            ...slot,
            driveId: drive.id,
            driveTitle: drive.title,
            driveDate: drive.driveDate,
            centerName: drive.center?.name || drive.centerName,
            centerCity: slot.centerCity || drive.center?.city || drive.centerCity,
            startDateTime,
            endDate: endDateTime,
            endDateTime,
            slotStatus: slot.slotStatus || getRealtimeStatus(startDateTime, endDateTime)
          });
        });
      });

      setAvailableSlots(
        slots.filter((slot) => slot.capacity > (slot.bookedCount || 0) && getRealtimeStatus(slot.startDateTime, slot.endDateTime) !== "EXPIRED")
      );
    } finally {
      slotsRequestInFlightRef.current = false;
    }
  }, [publicDrives]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    const unsubscribe = subscribeToDataUpdates(() => {
      loadData({ silent: true });
    });
    return unsubscribe;
  }, [loadData]);

  useEffect(() => {
    if (activeTab === "slots" || activeTab === "book") {
      loadAvailableSlots();
    }
  }, [activeTab, loadAvailableSlots]);

  const buildBookingPayload = (slotId) => {
    const selectedSlot = availableSlots.find((slot) => slot.id === slotId);
    return {
      slotId,
      driveId: selectedSlot?.driveId
    };
  };

  const downloadReceipt = (booking) => {
    const doc = new jsPDF();
    doc.setFontSize(18);
    doc.text("Vaccination Booking Receipt", 20, 20);
    doc.setFontSize(12);
    doc.text(`Booking ID: ${booking.id}`, 20, 40);
    doc.text(`Drive: ${booking.driveName || "-"}`, 20, 50);
    doc.text(`Center: ${booking.centerName || "-"}`, 20, 60);
    doc.text(`Slot Time: ${booking.slotTime ? new Date(booking.slotTime).toLocaleString() : "-"}`, 20, 70);
    doc.text(`Appointment: ${booking.assignedTime ? new Date(booking.assignedTime).toLocaleString() : "-"}`, 20, 80);
    doc.text(`Status: ${booking.status || "-"}`, 20, 90);
    doc.save(`booking-receipt-${booking.id}.pdf`);
  };

  const bookSlot = async (slotId) => {
    try {
      const payload = buildBookingPayload(slotId);
      const response = await api.post("/user/bookings", payload);
      const booking = unwrapApiData(response);
      const assignedTime = booking?.assignedTime;
      setMsg(
        assignedTime
          ? `Booking request submitted successfully. Your appointment time: ${formatAppointmentTime(assignedTime)}`
          : "Booking request submitted successfully."
      );
      setForms({ slotId: "" });
      broadcastDataUpdated({ source: "user-bookings-book" });
      await refreshCatalog();
      await Promise.all([loadData({ silent: true }), loadAvailableSlots()]);
      setActiveTab("bookings");
    } catch (error) {
      setMsg(error.response?.data?.message || "Failed to book slot. Please try again.");
    }
  };

  const cancelBooking = async (id) => {
    try {
      await api.patch(`/user/bookings/${id}/cancel`);
      setMsg("Booking cancelled successfully.");
      setBookingToCancel(null);
      broadcastDataUpdated({ source: "user-bookings-cancel" });
      await refreshCatalog();
      await Promise.all([loadData({ silent: true }), loadAvailableSlots()]);
    } catch (error) {
      setMsg(error.response?.data?.message || "Failed to cancel booking.");
    }
  };

  const joinWaitlist = async (slotId) => {
    try {
      setWaitlistLoadingId(slotId);
      const response = await api.post(`/user/slots/${slotId}/waitlist`);
      const entry = unwrapApiData(response);
      setWaitlistEntries((current) => [entry, ...current.filter((item) => item.id !== entry.id)]);
      setMsg("Joined waitlist successfully.");
    } catch (error) {
      setMsg(error.response?.data?.message || "Failed to join waitlist.");
    } finally {
      setWaitlistLoadingId(null);
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
  const slotCityOptions = [...new Set(availableSlots.map((slot) => slot.centerCity || slot.drive?.center?.city || "").filter(Boolean))].sort();
  const filteredAvailableSlots = availableSlots.filter((slot) => !slotCityFilter || (slot.centerCity || slot.drive?.center?.city || "").toLowerCase() === slotCityFilter.toLowerCase());

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
      <Seo
        title="My Vaccination Bookings | VaxZone"
        description="Manage your vaccination bookings, slot availability, notifications, and certificate access from one secure dashboard."
        path="/user/bookings"
        noIndex
      />
      <div className="page-header rounded-3 mb-4 p-4">
        <div className="d-flex align-items-center flex-wrap gap-3">
          <div>
            <h2 className="mb-1"><i className="bi bi-calendar-check me-2"></i>My Bookings</h2>
            <p className="mb-0 opacity-75">Manage your vaccination bookings and view notifications</p>
          </div>
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
                      <th><i className="bi bi-alarm me-1"></i>Appointment</th>
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
                        <td><small>{booking.assignedTime ? formatAppointmentTime(booking.assignedTime) : "-"}</small></td>
                        <td><small>{booking.centerName || "-"}</small></td>
                        <td>{getStatusBadge(booking.status)}</td>
                        <td>
                          {(booking.status === "PENDING" || booking.status === "CONFIRMED") && (
                            <button className="btn btn-outline-danger btn-sm" title="Cancel" onClick={() => setBookingToCancel(booking)}>
                              <i className="bi bi-x-circle"></i>
                            </button>
                          )}
                          <button className="btn btn-outline-primary btn-sm ms-2" title="Download Receipt" onClick={() => downloadReceipt(booking)}>
                            <i className="bi bi-file-earmark-pdf"></i>
                          </button>
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
            <div className="p-3 border-bottom">
              <label className="form-label mb-1">Filter by city</label>
              <select className="form-select" value={slotCityFilter} onChange={(event) => setSlotCityFilter(event.target.value)}>
                <option value="">All cities</option>
                {slotCityOptions.map((city) => <option key={city} value={city}>{city}</option>)}
              </select>
            </div>
            {filteredAvailableSlots.length === 0 ? (
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
                      <th><i className="bi bi-clock me-1"></i>Start</th>
                      <th><i className="bi bi-clock-history me-1"></i>End</th>
                      <th><i className="bi bi-broadcast me-1"></i>Status</th>
                      <th><i className="bi bi-building me-1"></i>Center</th>
                      <th><i className="bi bi-people me-1"></i>Capacity</th>
                      <th><i className="bi bi-gear me-1"></i>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredAvailableSlots.map((slot) => (
                      <tr key={slot.id}>
                        <td><strong>#{slot.id}</strong></td>
                        <td>{slot.driveTitle || slot.drive?.title}</td>
                        <td><small>{slot.startDateTime ? new Date(slot.startDateTime).toLocaleString() : "-"}</small></td>
                        <td><small>{slot.endDateTime ? new Date(slot.endDateTime).toLocaleString() : "-"}</small></td>
                        <td>
                          <div className="d-flex flex-column gap-1">
                            <span className={`badge ${getStatusBadgeClass(getRealtimeStatus(slot.startDateTime, slot.endDateTime, now))}`}>
                              {getRealtimeStatus(slot.startDateTime, slot.endDateTime, now)}
                            </span>
                            <small className="text-muted">
                              {getCountdownLabel(
                                getRealtimeStatus(slot.startDateTime, slot.endDateTime, now),
                                slot.startDateTime,
                                slot.endDateTime,
                                now
                              )}
                            </small>
                          </div>
                        </td>
                        <td>
                          <small>{slot.centerName || slot.drive?.center?.name}</small>
                          <div className="text-muted small">{slot.centerCity || slot.drive?.center?.city || "-"}</div>
                        </td>
                        <td>{(slot.capacity || 0) - (slot.bookedCount || 0)} / {slot.capacity}</td>
                        <td>
                          {isSlotBookable(slot, now) ? (
                            <button className="btn btn-primary btn-sm" onClick={() => bookSlot(slot.id)} disabled={!isSlotBookable(slot, now)}>
                              <i className="bi bi-bookmark-plus me-1"></i>Book Now
                            </button>
                          ) : isAtCapacity(slot) ? (
                            <button className="btn btn-outline-warning btn-sm" onClick={() => joinWaitlist(slot.id)} disabled={waitlistLoadingId === slot.id}>
                              <i className="bi bi-hourglass-split me-1"></i>{waitlistLoadingId === slot.id ? "Joining..." : "Join Waitlist"}
                            </button>
                          ) : (
                            <button className="btn btn-secondary btn-sm" disabled>
                              <i className="bi bi-clock-history me-1"></i>Expired
                            </button>
                          )}
                          {slot.demandLevel === "HIGH_DEMAND" && <div className="text-danger small mt-1">🔥 High Demand</div>}
                          {slot.almostFull && slot.demandLevel !== "HIGH_DEMAND" && <div className="text-warning small mt-1">Almost Full</div>}
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

      {waitlistEntries.length > 0 && (
        <div className="card mt-4">
          <div className="card-header"><i className="bi bi-hourglass-split me-2"></i>My Waitlist</div>
          <div className="card-body p-0">
            <div className="table-responsive">
              <table className="table table-hover mb-0">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Drive</th>
                    <th>Center</th>
                    <th>Status</th>
                    <th>Joined</th>
                  </tr>
                </thead>
                <tbody>
                  {waitlistEntries.map((entry) => (
                    <tr key={entry.id}>
                      <td>#{entry.id}</td>
                      <td>{entry.driveName}</td>
                      <td>{entry.centerName} <small className="text-muted">{entry.centerCity}</small></td>
                      <td><span className={`badge ${entry.status === "PROMOTED" ? "bg-success" : "bg-warning text-dark"}`}>{entry.status}</span></td>
                      <td>{entry.createdAt ? new Date(entry.createdAt).toLocaleString() : "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
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
                {notifications.slice(0, 20).map((notification) => {
                  const copy = getNotificationCopy(notification);

                  return (
                    <li key={notification.id} className="list-group-item d-flex justify-content-between align-items-start p-3">
                      <div>
                        <div className="fw-semibold">{notification.title || `${notification.type} Notification`}</div>
                        <div className="text-muted small">{copy.message}</div>
                        <div className="small mt-1">{copy.followUp}</div>
                      </div>
                      <small className="text-muted">{notification.createdAt ? new Date(notification.createdAt).toLocaleString() : ""}</small>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>
      )}
      <ModalPopup
        show={Boolean(bookingToCancel)}
        title="Cancel booking"
        body={bookingToCancel ? `Cancel booking #${bookingToCancel.id} for ${bookingToCancel.driveName || "this drive"}?` : ""}
        confirmLabel="Yes, cancel"
        cancelLabel="Keep booking"
        confirmVariant="danger"
        onConfirm={() => cancelBooking(bookingToCancel.id)}
        onCancel={() => setBookingToCancel(null)}
      />
    </div>
  );
}
