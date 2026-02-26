import React, { useEffect, useState } from 'react';
import { Table, Button, Spinner, Alert, Card } from 'react-bootstrap';
import { bookingAPI } from '../api/axios';

const MyBookings = () => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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
        alert('Booking cancelled successfully!');
      } catch (err) {
        alert(err.response?.data?.message || 'Failed to cancel booking');
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

  if (loading) {
    return (
      <div className="loading-spinner">
        <Spinner animation="border" role="status" variant="primary">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  return (
    <div className="my-bookings">
      <h2 className="mb-4">📋 My Bookings</h2>
      
      {error && (
        <Alert variant="danger" className="mb-4">
          {error}
        </Alert>
      )}
      
      {/* Stats Cards */}
      <div className="row mb-4">
        <div className="col-md-3">
          <Card className="stat-card">
            <Card.Body className="text-center">
              <Card.Title>Total</Card.Title>
              <div className="stat-value">{bookings.length}</div>
            </Card.Body>
          </Card>
        </div>
        <div className="col-md-3">
          <Card className="stat-card">
            <Card.Body className="text-center">
              <Card.Title>Pending</Card.Title>
              <div className="stat-value text-warning">
                {bookings.filter(b => b.status === 'PENDING').length}
              </div>
            </Card.Body>
          </Card>
        </div>
        <div className="col-md-3">
          <Card className="stat-card">
            <Card.Body className="text-center">
              <Card.Title>Approved</Card.Title>
              <div className="stat-value text-success">
                {bookings.filter(b => b.status === 'APPROVED').length}
              </div>
            </Card.Body>
          </Card>
        </div>
        <div className="col-md-3">
          <Card className="stat-card">
            <Card.Body className="text-center">
              <Card.Title>Completed</Card.Title>
              <div className="stat-value text-info">
                {bookings.filter(b => b.status === 'COMPLETED').length}
              </div>
            </Card.Body>
          </Card>
        </div>
      </div>

      {bookings.length === 0 ? (
        <Alert variant="info">
          You haven't made any bookings yet. Go to the Dashboard to book your vaccination slot!
        </Alert>
      ) : (
        <Card>
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
              {bookings.map((booking) => (
                <tr key={booking.id}>
                  <td>#{booking.id}</td>
                  <td>
                    <strong>{booking.driveName}</strong>
                  </td>
                  <td>{booking.vaccineName}</td>
                  <td>{booking.centerName}</td>
                  <td>{booking.appointmentDate || 'N/A'}</td>
                  <td>{booking.appointmentTime || 'N/A'}</td>
                  <td>{getStatusBadge(booking.status)}</td>
                  <td>
                    {(booking.status === 'PENDING' || booking.status === 'APPROVED') && (
                      <Button 
                        variant="outline-danger" 
                        size="sm"
                        onClick={() => handleCancel(booking.id)}
                      >
                        Cancel
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </Card>
      )}
    </div>
  );
};

export default MyBookings;
