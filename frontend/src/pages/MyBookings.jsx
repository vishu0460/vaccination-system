import React, { useEffect, useState } from 'react';
import { Table, Button, Spinner, Alert, Card, Row, Col } from 'react-bootstrap';
import { bookingAPI } from '../api/axios';
import { useToast } from '../components/Toast';
import { SkeletonTable, DashboardSkeleton } from '../components/Skeleton';

const MyBookings = () => {
  const toast = useToast();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('ALL');

  useEffect(() => {
    const fetchBookings = async () => {
      try {
        setLoading(true);
        const res = await bookingAPI.getMyBookings();
        setBookings(res.data);
      } catch (err) {
        setError('Failed to load bookings. Please try again.');
        console.error('Error fetching bookings:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchBookings();
  }, []);

  const handleCancel = async (id) => {
    if (window.confirm('Are you sure you want to cancel this booking?')) {
      try {
        await bookingAPI.cancelBooking(id);
        setBookings(bookings.map(b => 
          b.id === id ? { ...b, status: 'CANCELLED' } : b
        ));
        toast.success('Booking cancelled successfully!');
      } catch (err) {
        toast.error(err.response?.data?.message || 'Failed to cancel booking');
      }
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

  // Filter bookings
  const filteredBookings = bookings.filter(b => {
    if (filter === 'ALL') return true;
    return b.status === filter;
  });

  if (loading) {
    return <DashboardSkeleton />;
  }

  return (
    <div className="my-bookings fade-in">
      <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-3">
        <h2 className="mb-0">📋 My Bookings</h2>
      </div>
      
      {error && (
        <Alert variant="danger" className="mb-4">
          {error}
        </Alert>
      )}
      
      {/* Stats Cards */}
      <Row className="mb-4">
        <Col md={3}>
          <Card className="stat-card h-100">
            <Card.Body className="text-center">
              <div className="stat-icon">📋</div>
              <Card.Title>Total</Card.Title>
              <h3>{bookings.length}</h3>
            </Card.Body>
          </Card>
        </Col>
        <Col md={3}>
          <Card className="stat-card h-100">
            <Card.Body className="text-center">
              <div className="stat-icon">⏳</div>
              <Card.Title>Pending</Card.Title>
              <h3 className="text-warning">
                {bookings.filter(b => b.status === 'PENDING').length}
              </h3>
            </Card.Body>
          </Card>
        </Col>
        <Col md={3}>
          <Card className="stat-card h-100">
            <Card.Body className="text-center">
              <div className="stat-icon">✅</div>
              <Card.Title>Approved</Card.Title>
              <h3 className="text-success">
                {bookings.filter(b => b.status === 'APPROVED').length}
              </h3>
            </Card.Body>
          </Card>
        </Col>
        <Col md={3}>
          <Card className="stat-card h-100">
            <Card.Body className="text-center">
              <div className="stat-icon">🎉</div>
              <Card.Title>Completed</Card.Title>
              <h3 className="text-info">
                {bookings.filter(b => b.status === 'COMPLETED').length}
              </h3>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Filter Buttons */}
      <div className="mb-3">
        <Button 
          variant={filter === 'ALL' ? 'primary' : 'outline-primary'} 
          size="sm" 
          className="me-2"
          onClick={() => setFilter('ALL')}
        >
          All ({bookings.length})
        </Button>
        <Button 
          variant={filter === 'PENDING' ? 'warning' : 'outline-warning'} 
          size="sm" 
          className="me-2"
          onClick={() => setFilter('PENDING')}
        >
          Pending ({bookings.filter(b => b.status === 'PENDING').length})
        </Button>
        <Button 
          variant={filter === 'APPROVED' ? 'success' : 'outline-success'} 
          size="sm" 
          className="me-2"
          onClick={() => setFilter('APPROVED')}
        >
          Approved ({bookings.filter(b => b.status === 'APPROVED').length})
        </Button>
        <Button 
          variant={filter === 'COMPLETED' ? 'info' : 'outline-info'} 
          size="sm" 
          className="me-2"
          onClick={() => setFilter('COMPLETED')}
        >
          Completed ({bookings.filter(b => b.status === 'COMPLETED').length})
        </Button>
        <Button 
          variant={filter === 'CANCELLED' ? 'danger' : 'outline-danger'} 
          size="sm"
          onClick={() => setFilter('CANCELLED')}
        >
          Cancelled ({bookings.filter(b => b.status === 'CANCELLED').length})
        </Button>
      </div>

      {filteredBookings.length === 0 ? (
        <Alert variant="info">
          {filter === 'ALL' 
            ? "You haven't made any bookings yet. Go to the Dashboard to book your vaccination slot!" 
            : `No ${filter.toLowerCase()} bookings found.`}
        </Alert>
      ) : (
        <Card>
          <div className="table-responsive">
            <Table responsive className="mb-0">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Drive</th>
                  <th>Vaccine</th>
                  <th>Center</th>
                  <th>Date</th>
                  <th>Time</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredBookings.map((booking) => (
                  <tr key={booking.id}>
                    <td>#{booking.id}</td>
                    <td>
                      <strong>{booking.driveName || booking.drive?.title}</strong>
                    </td>
                    <td>{booking.vaccineName || 'N/A'}</td>
                    <td>{booking.centerName || booking.center?.name}</td>
                    <td>{booking.appointmentDate || booking.slotDate || 'N/A'}</td>
                    <td>{booking.appointmentTime || booking.slotTime || 'N/A'}</td>
                    <td>{getStatusBadge(booking.status)}</td>
                    <td>
                      {(booking.status === 'PENDING' || booking.status === 'APPROVED') && (
                        <Button 
                          variant="outline-danger" 
                          size="sm"
                          onClick={() => handleCancel(booking.id)}
                        >
                          ❌ Cancel
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </div>
        </Card>
      )}
    </div>
  );
};

export default MyBookings;
