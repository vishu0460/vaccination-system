import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Button, Table, Spinner, Modal, Alert } from 'react-bootstrap';
import { publicAPI, bookingAPI } from '../api/axios';
import { useToast } from '../components/Toast';
import { SkeletonCard, SkeletonTable, DashboardSkeleton } from '../components/Skeleton';

const Dashboard = () => {
  const toast = useToast();
  const [user] = useState(JSON.parse(localStorage.getItem('user') || '{}'));
  const [drives, setDrives] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [selectedDrive, setSelectedDrive] = useState(null);
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [showSlotModal, setShowSlotModal] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [drivesRes, bookingsRes] = await Promise.all([
        publicAPI.getDrives(),
        bookingAPI.getMyBookings()
      ]);
      setDrives(drivesRes.data);
      setBookings(bookingsRes.data);
    } catch (error) {
      console.error('Error fetching data:', error);
      toast.error('Failed to load data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const fetchSlots = async (driveId) => {
    try {
      setSlotsLoading(true);
      const res = await publicAPI.getSlots(driveId);
      setSlots(res.data);
    } catch (error) {
      console.error('Error fetching slots:', error);
      toast.error('Failed to load slots');
    } finally {
      setSlotsLoading(false);
    }
  };

  const handleBookSlot = async (slot) => {
    setSelectedSlot(slot);
    setShowSlotModal(true);
  };

  const confirmBooking = async () => {
    if (!selectedSlot) return;
    try {
      setBookingLoading(true);
      await bookingAPI.createBooking({ 
        slotId: selectedSlot.id,
        driveId: selectedDrive.id
      });
      toast.success('🎉 Booking successful! You will receive a confirmation shortly.');
      setShowSlotModal(false);
      setSelectedSlot(null);
      fetchData();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Booking failed. Please try again.');
    } finally {
      setBookingLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      'PENDING': { variant: 'warning', icon: '⏳' },
      'APPROVED': { variant: 'success', icon: '✅' },
      'COMPLETED': { variant: 'info', icon: '🎉' },
      'CANCELLED': { variant: 'danger', icon: '❌' }
    };
    const { variant, icon } = statusMap[status] || { variant: 'secondary', icon: '❓' };
    return <span className={`badge bg-${variant}`}>{icon} {status}</span>;
  };

  if (loading) {
    return <DashboardSkeleton />;
  }

  return (
    <div className="dashboard fade-in">
      <div className="dashboard-header">
        <div>
          <h2 className="dashboard-title">Welcome back, {user.firstName}! 👋</h2>
          <p className="text-muted mb-0">Here's your vaccination booking overview</p>
        </div>
      </div>
      
      {/* Stats Cards */}
      <Row className="mb-4">
        <Col md={4}>
          <Card className="stat-card h-100">
            <Card.Body>
              <div className="stat-icon">📋</div>
              <Card.Title>My Bookings</Card.Title>
              <h3>{bookings.length}</h3>
              <small className="text-muted">Total appointments</small>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card className="stat-card h-100">
            <Card.Body>
              <div className="stat-icon">✅</div>
              <Card.Title>Upcoming</Card.Title>
              <h3 className="text-success">
                {bookings.filter(b => b.status === 'APPROVED').length}
              </h3>
              <small className="text-muted">Confirmed appointments</small>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card className="stat-card h-100">
            <Card.Body>
              <div className="stat-icon">🎉</div>
              <Card.Title>Completed</Card.Title>
              <h3 className="text-info">
                {bookings.filter(b => b.status === 'COMPLETED').length}
              </h3>
              <small className="text-muted">Completed vaccinations</small>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Available Drives */}
      <section className="mb-5">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h3 className="mb-0">💉 Available Vaccination Drives</h3>
        </div>
        
        {drives.length === 0 ? (
          <Alert variant="info">No drives available at the moment.</Alert>
        ) : (
          <Row>
            {drives.map((drive, index) => (
              <Col md={4} key={drive.id} className="mb-3">
                <Card className="h-100 drive-card" style={{ animationDelay: `${index * 0.1}s` }}>
                  <Card.Header className="d-flex justify-content-between align-items-center">
                    <span className="badge bg-primary">{drive.vaccineName || 'Vaccine'}</span>
                    <small className="text-muted">{drive.vaccineManufacturer || 'Government Approved'}</small>
                  </Card.Header>
                  <Card.Body>
                    <Card.Title>{drive.title || drive.name}</Card.Title>
                    <Card.Subtitle className="mb-2 text-muted">
                      📍 {drive.center?.name || drive.centerName}
                    </Card.Subtitle>
                    <Card.Text>
                      <p className="mb-1">📅 {drive.driveDate || drive.startDate}</p>
                      <p className="mb-0">👥 Age: {drive.minAge || 18} - {drive.maxAge || 60} years</p>
                    </Card.Text>
                  </Card.Body>
                  <Card.Footer>
                    <div className="d-flex justify-content-between align-items-center">
                      <span className={drive.availableSlots > 0 ? 'text-success' : 'text-danger'}>
                        💉 {drive.availableSlots || 0} slots available
                      </span>
                      {drive.availableSlots > 0 && (
                        <Button 
                          variant="primary" 
                          size="sm"
                          onClick={() => {
                            setSelectedDrive(drive);
                            fetchSlots(drive.id);
                            setShowSlotModal(true);
                          }}
                        >
                          Book Now
                        </Button>
                      )}
                    </div>
                  </Card.Footer>
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </section>

      {/* Slot Selection Modal */}
      <Modal show={showSlotModal} onHide={() => setShowSlotModal(false)} size="lg" centered>
        <Modal.Header closeButton>
          <Modal.Title>
            {selectedDrive ? `Select Slot for ${selectedDrive.title || selectedDrive.name}` : 'Available Slots'}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {slotsLoading ? (
            <div className="text-center py-4">
              <Spinner animation="border" variant="primary" />
              <p className="mt-2">Loading slots...</p>
            </div>
          ) : slots.length === 0 ? (
            <Alert variant="warning">No slots available for this drive.</Alert>
          ) : (
            <div className="table-responsive">
              <Table striped bordered hover responsive>
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Time</th>
                    <th>Available</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {slots.map((slot) => (
                    <tr key={slot.id}>
                      <td>{slot.slotDate}</td>
                      <td>{slot.slotTime}</td>
                      <td>
                        <span className={slot.availableCapacity > 0 ? 'text-success' : 'text-danger'}>
                          {slot.availableCapacity}
                        </span>
                      </td>
                      <td>
                        {slot.availableCapacity > 0 && (
                          <Button 
                            variant="success" 
                            size="sm"
                            onClick={() => handleBookSlot(slot)}
                          >
                            Select
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowSlotModal(false)}>
            Close
          </Button>
        </Modal.Footer>
      </Modal>

      {/* Booking Confirmation Modal */}
      <Modal show={!!selectedSlot && showSlotModal} onHide={() => setShowSlotModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Confirm Booking</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {selectedSlot && (
            <>
              <p><strong>Drive:</strong> {selectedDrive?.title || selectedDrive?.name}</p>
              <p><strong>Center:</strong> {selectedDrive?.center?.name || selectedDrive?.centerName}</p>
              <p><strong>Date:</strong> {selectedSlot.slotDate}</p>
              <p><strong>Time:</strong> {selectedSlot.slotTime}</p>
              <Alert variant="info">
                Please arrive at the center 15 minutes before your scheduled time.
              </Alert>
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowSlotModal(false)}>
            Cancel
          </Button>
          <Button variant="primary" onClick={confirmBooking} disabled={bookingLoading}>
            {bookingLoading ? 'Booking...' : 'Confirm Booking'}
          </Button>
        </Modal.Footer>
      </Modal>

      {/* My Bookings Section */}
      <section>
        <h3 className="mb-4">📋 My Bookings</h3>
        {bookings.length === 0 ? (
          <Alert variant="info">
            You haven't made any bookings yet. Browse available drives above to book your vaccination slot!
          </Alert>
        ) : (
          <Card>
            <div className="table-responsive">
              <Table responsive className="mb-0">
                <thead>
                  <tr>
                    <th>Drive</th>
                    <th>Center</th>
                    <th>Date</th>
                    <th>Time</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {bookings.map((booking) => (
                    <tr key={booking.id}>
                      <td>
                        <strong>{booking.driveName || booking.drive?.title}</strong><br/>
                        <small className="text-muted">{booking.vaccineName || 'Vaccination'}</small>
                      </td>
                      <td>{booking.centerName || booking.center?.name}</td>
                      <td>{booking.appointmentDate || booking.slotDate || 'N/A'}</td>
                      <td>{booking.appointmentTime || booking.slotTime || 'N/A'}</td>
                      <td>{getStatusBadge(booking.status)}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          </Card>
        )}
      </section>
    </div>
  );
};

export default Dashboard;
