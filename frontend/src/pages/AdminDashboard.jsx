import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Table, Button, Spinner, Alert, Modal, Form } from 'react-bootstrap';
import { adminAPI, publicAPI, bookingAPI } from '../api/axios';

const AdminDashboard = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ totalUsers: 0, totalCenters: 0, totalDrives: 0, totalBookings: 0 });
  const [bookings, setBookings] = useState([]);
  const [users, setUsers] = useState([]);
  const [drives, setDrives] = useState([]);
  const [centers, setCenters] = useState([]);
  const [activeTab, setActiveTab] = useState('bookings');
  const [showDriveModal, setShowDriveModal] = useState(false);
  const [showCenterModal, setShowCenterModal] = useState(false);
  const [newDrive, setNewDrive] = useState({ name: '', vaccineName: '', description: '', centerId: '', startDate: '', endDate: '', startTime: '', endTime: '', minAge: 18, maxAge: 60, dosesRequired: 1 });
  const [newCenter, setNewCenter] = useState({ name: '', address: '', city: '', state: '', pincode: '', phone: '', email: '', capacityPerDay: 100 });

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const [bookingsRes, usersRes, drivesRes, centersRes] = await Promise.all([
        adminAPI.getPendingBookings(),
        adminAPI.getAllUsers(),
        publicAPI.getDrives(),
        publicAPI.getCenters()
      ]);
      
      setBookings(bookingsRes.data);
      setUsers(usersRes.data);
      setDrives(drivesRes.data);
      setCenters(centersRes.data);
      
      setStats({
        totalUsers: usersRes.data.length,
        totalCenters: centersRes.data.length,
        totalDrives: drivesRes.data.length,
        totalBookings: bookingsRes.data.length
      });
    } catch (error) {
      console.error('Error fetching dashboard:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    try {
      await bookingAPI.approveBooking(id);
      fetchDashboardData();
    } catch (error) {
      alert('Failed to approve booking');
    }
  };

  const handleReject = async (id) => {
    const reason = prompt('Enter rejection reason:');
    if (reason) {
      try {
        await bookingAPI.rejectBooking(id, reason);
        fetchDashboardData();
      } catch (error) {
        alert('Failed to reject booking');
      }
    }
  };

  const handleDeleteUser = async (id) => {
    if (window.confirm('Are you sure you want to deactivate this user?')) {
      try {
        await adminAPI.deleteUser(id);
        fetchDashboardData();
      } catch (error) {
        alert('Failed to delete user');
      }
    }
  };

  const handleDeleteDrive = async (id) => {
    if (window.confirm('Are you sure you want to delete this drive?')) {
      try {
        await adminAPI.deleteDrive(id);
        fetchDashboardData();
      } catch (error) {
        alert('Failed to delete drive');
      }
    }
  };

  const handleDeleteCenter = async (id) => {
    if (window.confirm('Are you sure you want to delete this center?')) {
      try {
        await adminAPI.deleteCenter(id);
        fetchDashboardData();
      } catch (error) {
        alert('Failed to delete center');
      }
    }
  };

  const handleCreateDrive = async (e) => {
    e.preventDefault();
    try {
      await adminAPI.createDrive(newDrive);
      setShowDriveModal(false);
      setNewDrive({ name: '', vaccineName: '', description: '', centerId: '', startDate: '', endDate: '', startTime: '', endTime: '', minAge: 18, maxAge: 60, dosesRequired: 1 });
      fetchDashboardData();
    } catch (error) {
      alert('Failed to create drive');
    }
  };

  const handleCreateCenter = async (e) => {
    e.preventDefault();
    try {
      await adminAPI.createCenter(newCenter);
      setShowCenterModal(false);
      setNewCenter({ name: '', address: '', city: '', state: '', pincode: '', phone: '', email: '', capacityPerDay: 100 });
      fetchDashboardData();
    } catch (error) {
      alert('Failed to create center');
    }
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
    <div>
      <h2 className="mb-4">Admin Dashboard</h2>
      
      <Row className="mb-4">
        <Col md={3}>
          <Card className="stat-card">
            <Card.Body>
              <Card.Title>Total Users</Card.Title>
              <h3>{stats.totalUsers}</h3>
            </Card.Body>
          </Card>
        </Col>
        <Col md={3}>
          <Card className="stat-card">
            <Card.Body>
              <Card.Title>Total Centers</Card.Title>
              <h3>{stats.totalCenters}</h3>
            </Card.Body>
          </Card>
        </Col>
        <Col md={3}>
          <Card className="stat-card">
            <Card.Body>
              <Card.Title>Total Drives</Card.Title>
              <h3>{stats.totalDrives}</h3>
            </Card.Body>
          </Card>
        </Col>
        <Col md={3}>
          <Card className="stat-card">
            <Card.Body>
              <Card.Title>Pending Bookings</Card.Title>
              <h3>{stats.totalBookings}</h3>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Row className="mb-3">
        <Col>
          <div className="d-flex gap-2">
            <Button variant={activeTab === 'bookings' ? 'primary' : 'outline-primary'} onClick={() => setActiveTab('bookings')}>
              Pending Bookings
            </Button>
            <Button variant={activeTab === 'users' ? 'primary' : 'outline-primary'} onClick={() => setActiveTab('users')}>
              Users
            </Button>
            <Button variant={activeTab === 'drives' ? 'primary' : 'outline-primary'} onClick={() => setActiveTab('drives')}>
              Drives
            </Button>
            <Button variant={activeTab === 'centers' ? 'primary' : 'outline-primary'} onClick={() => setActiveTab('centers')}>
              Centers
            </Button>
          </div>
        </Col>
      </Row>

      {activeTab === 'bookings' && (
        <>
          <h3>Pending Bookings</h3>
          {bookings.length === 0 ? (
            <Alert variant="info">No pending bookings</Alert>
          ) : (
            <Table striped bordered hover responsive>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>User</th>
                  <th>Drive</th>
                  <th>Center</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {bookings.map((booking) => (
                  <tr key={booking.id}>
                    <td>{booking.id}</td>
                    <td>{booking.userName}</td>
                    <td>{booking.driveName}</td>
                    <td>{booking.centerName}</td>
                    <td>{booking.appointmentDate || 'N/A'}</td>
                    <td>
                      <span className={`badge bg-${booking.status === 'PENDING' ? 'warning' : booking.status === 'APPROVED' ? 'success' : 'danger'}`}>
                        {booking.status}
                      </span>
                    </td>
                    <td>
                      <Button variant="success" size="sm" className="me-2" onClick={() => handleApprove(booking.id)}>
                        Approve
                      </Button>
                      <Button variant="danger" size="sm" onClick={() => handleReject(booking.id)}>
                        Reject
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </>
      )}

      {activeTab === 'users' && (
        <>
          <h3>All Users</h3>
          {users.length === 0 ? (
            <Alert variant="info">No users found</Alert>
          ) : (
            <Table striped bordered hover responsive>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Aadhar</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{user.firstName} {user.lastName}</td>
                    <td>{user.email}</td>
                    <td>{user.phone || 'N/A'}</td>
                    <td>{user.aadharNumber || 'N/A'}</td>
                    <td>
                      <span className={`badge bg-${user.isActive ? 'success' : 'danger'}`}>
                        {user.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <Button variant="danger" size="sm" onClick={() => handleDeleteUser(user.id)}>
                        Deactivate
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </>
      )}

      {activeTab === 'drives' && (
        <>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h3>Vaccination Drives</h3>
            <Button variant="primary" onClick={() => setShowDriveModal(true)}>
              + Create Drive
            </Button>
          </div>
          {drives.length === 0 ? (
            <Alert variant="info">No drives found</Alert>
          ) : (
            <Table striped bordered hover responsive>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Vaccine</th>
                  <th>Center</th>
                  <th>Dates</th>
                  <th>Available</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {drives.map((drive) => (
                  <tr key={drive.id}>
                    <td>{drive.id}</td>
                    <td>{drive.name}</td>
                    <td>{drive.vaccineName}</td>
                    <td>{drive.centerName}</td>
                    <td>{drive.startDate} to {drive.endDate}</td>
                    <td>{drive.availableSlots}</td>
                    <td>
                      <Button variant="danger" size="sm" onClick={() => handleDeleteDrive(drive.id)}>
                        Delete
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </>
      )}

      {activeTab === 'centers' && (
        <>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h3>Vaccination Centers</h3>
            <Button variant="primary" onClick={() => setShowCenterModal(true)}>
              + Create Center
            </Button>
          </div>
          {centers.length === 0 ? (
            <Alert variant="info">No centers found</Alert>
          ) : (
            <Table striped bordered hover responsive>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>City</th>
                  <th>Address</th>
                  <th>Phone</th>
                  <th>Capacity</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {centers.map((center) => (
                  <tr key={center.id}>
                    <td>{center.id}</td>
                    <td>{center.name}</td>
                    <td>{center.city}</td>
                    <td>{center.address}</td>
                    <td>{center.phone}</td>
                    <td>{center.capacityPerDay}</td>
                    <td>
                      <Button variant="danger" size="sm" onClick={() => handleDeleteCenter(center.id)}>
                        Delete
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </>
      )}

      {/* Create Drive Modal */}
      <Modal show={showDriveModal} onHide={() => setShowDriveModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Create New Drive</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateDrive}>
          <Modal.Body>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Drive Name</Form.Label>
                  <Form.Control type="text" value={newDrive.name} onChange={(e) => setNewDrive({...newDrive, name: e.target.value})} required />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Vaccine Name</Form.Label>
                  <Form.Control type="text" value={newDrive.vaccineName} onChange={(e) => setNewDrive({...newDrive, vaccineName: e.target.value})} required />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control as="textarea" rows={2} value={newDrive.description} onChange={(e) => setNewDrive({...newDrive, description: e.target.value})} />
            </Form.Group>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Center</Form.Label>
                  <Form.Select value={newDrive.centerId} onChange={(e) => setNewDrive({...newDrive, centerId: e.target.value})} required>
                    <option value="">Select Center</option>
                    {centers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>Min Age</Form.Label>
                  <Form.Control type="number" value={newDrive.minAge} onChange={(e) => setNewDrive({...newDrive, minAge: e.target.value})} required />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>Max Age</Form.Label>
                  <Form.Control type="number" value={newDrive.maxAge} onChange={(e) => setNewDrive({...newDrive, maxAge: e.target.value})} required />
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>Start Date</Form.Label>
                  <Form.Control type="date" value={newDrive.startDate} onChange={(e) => setNewDrive({...newDrive, startDate: e.target.value})} required />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>End Date</Form.Label>
                  <Form.Control type="date" value={newDrive.endDate} onChange={(e) => setNewDrive({...newDrive, endDate: e.target.value})} required />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>Start Time</Form.Label>
                  <Form.Control type="time" value={newDrive.startTime} onChange={(e) => setNewDrive({...newDrive, startTime: e.target.value})} required />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>End Time</Form.Label>
                  <Form.Control type="time" value={newDrive.endTime} onChange={(e) => setNewDrive({...newDrive, endTime: e.target.value})} required />
                </Form.Group>
              </Col>
            </Row>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowDriveModal(false)}>Close</Button>
            <Button variant="primary" type="submit">Create Drive</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Create Center Modal */}
      <Modal show={showCenterModal} onHide={() => setShowCenterModal(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Create New Center</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateCenter}>
          <Modal.Body>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Center Name</Form.Label>
                  <Form.Control type="text" value={newCenter.name} onChange={(e) => setNewCenter({...newCenter, name: e.target.value})} required />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Email</Form.Label>
                  <Form.Control type="email" value={newCenter.email} onChange={(e) => setNewCenter({...newCenter, email: e.target.value})} required />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3">
              <Form.Label>Address</Form.Label>
              <Form.Control type="text" value={newCenter.address} onChange={(e) => setNewCenter({...newCenter, address: e.target.value})} required />
            </Form.Group>
            <Row>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>City</Form.Label>
                  <Form.Control type="text" value={newCenter.city} onChange={(e) => setNewCenter({...newCenter, city: e.target.value})} required />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>State</Form.Label>
                  <Form.Control type="text" value={newCenter.state} onChange={(e) => setNewCenter({...newCenter, state: e.target.value})} required />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Pincode</Form.Label>
                  <Form.Control type="text" value={newCenter.pincode} onChange={(e) => setNewCenter({...newCenter, pincode: e.target.value})} required />
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Phone</Form.Label>
                  <Form.Control type="text" value={newCenter.phone} onChange={(e) => setNewCenter({...newCenter, phone: e.target.value})} required />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Capacity Per Day</Form.Label>
                  <Form.Control type="number" value={newCenter.capacityPerDay} onChange={(e) => setNewCenter({...newCenter, capacityPerDay: e.target.value})} required />
                </Form.Group>
              </Col>
            </Row>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowCenterModal(false)}>Close</Button>
            <Button variant="primary" type="submit">Create Center</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
};

export default AdminDashboard;
