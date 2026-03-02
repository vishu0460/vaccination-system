import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Table, Button, Spinner, Alert, Modal, Form, Nav, Tabs, Tab } from 'react-bootstrap';
import { adminAPI, publicAPI, bookingAPI } from '../api/axios';
import { useToast } from '../components/Toast';
import { DashboardSkeleton, SkeletonTable } from '../components/Skeleton';

const AdminDashboard = () => {
  const toast = useToast();
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ totalUsers: 0, totalCenters: 0, totalDrives: 0, totalBookings: 0 });
  const [bookings, setBookings] = useState([]);
  const [users, setUsers] = useState([]);
  const [drives, setDrives] = useState([]);
  const [centers, setCenters] = useState([]);
  const [activeTab, setActiveTab] = useState('bookings');
  const [showDriveModal, setShowDriveModal] = useState(false);
  const [showCenterModal, setShowCenterModal] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [newDrive, setNewDrive] = useState({ 
    name: '', 
    vaccineName: '', 
    description: '', 
    centerId: '', 
    startDate: '', 
    endDate: '', 
    startTime: '', 
    endTime: '', 
    minAge: 18, 
    maxAge: 60, 
    dosesRequired: 1 
  });
  const [newCenter, setNewCenter] = useState({ 
    name: '', 
    address: '', 
    city: '', 
    state: '', 
    pincode: '', 
    phone: '', 
    email: '', 
    capacityPerDay: 100 
  });

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
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    try {
      await bookingAPI.approveBooking(id);
      toast.success('Booking approved successfully!');
      fetchDashboardData();
    } catch (error) {
      toast.error('Failed to approve booking');
    }
  };

  const handleReject = async (id) => {
    const reason = prompt('Enter rejection reason:');
    if (reason) {
      try {
        await bookingAPI.rejectBooking(id, reason);
        toast.success('Booking rejected');
        fetchDashboardData();
      } catch (error) {
        toast.error('Failed to reject booking');
      }
    }
  };

  const handleDeleteUser = async (id) => {
    if (window.confirm('Are you sure you want to deactivate this user?')) {
      try {
        await adminAPI.deleteUser(id);
        toast.success('User deactivated successfully');
        fetchDashboardData();
      } catch (error) {
        toast.error('Failed to deactivate user');
      }
    }
  };

  const handleDeleteDrive = async (id) => {
    if (window.confirm('Are you sure you want to delete this drive?')) {
      try {
        await adminAPI.deleteDrive(id);
        toast.success('Drive deleted successfully');
        fetchDashboardData();
      } catch (error) {
        toast.error('Failed to delete drive');
      }
    }
  };

  const handleDeleteCenter = async (id) => {
    if (window.confirm('Are you sure you want to delete this center?')) {
      try {
        await adminAPI.deleteCenter(id);
        toast.success('Center deleted successfully');
        fetchDashboardData();
      } catch (error) {
        toast.error('Failed to delete center');
      }
    }
  };

  const handleCreateDrive = async (e) => {
    e.preventDefault();
    try {
      await adminAPI.createDrive(newDrive);
      toast.success('Drive created successfully!');
      setShowDriveModal(false);
      setNewDrive({ name: '', vaccineName: '', description: '', centerId: '', startDate: '', endDate: '', startTime: '', endTime: '', minAge: 18, maxAge: 60, dosesRequired: 1 });
      fetchDashboardData();
    } catch (error) {
      toast.error('Failed to create drive');
    }
  };

  const handleCreateCenter = async (e) => {
    e.preventDefault();
    try {
      await adminAPI.createCenter(newCenter);
      toast.success('Center created successfully!');
      setShowCenterModal(false);
      setNewCenter({ name: '', address: '', city: '', state: '', pincode: '', phone: '', email: '', capacityPerDay: 100 });
      fetchDashboardData();
    } catch (error) {
      toast.error('Failed to create center');
    }
  };

  // Filter functions
  const filterBySearch = (items) => {
    if (!searchTerm) return items;
    return items.filter(item => 
      JSON.stringify(item).toLowerCase().includes(searchTerm.toLowerCase())
    );
  };

  if (loading) {
    return <DashboardSkeleton />;
  }

  return (
    <div className="admin-dashboard fade-in">
      <div className="dashboard-header">
        <div>
          <h2 className="dashboard-title">Admin Dashboard</h2>
          <p className="text-muted mb-0">Manage your vaccination system</p>
        </div>
        <div className="dashboard-actions">
          <Button variant="primary" onClick={() => setShowDriveModal(true)}>
            ➕ Create Drive
          </Button>
          <Button variant="success" onClick={() => setShowCenterModal(true)}>
            ➕ Add Center
          </Button>
        </div>
      </div>
      
      {/* Stats Cards */}
      <Row className="mb-4">
        <Col md={3}>
          <Card className="stat-card h-100">
            <Card.Body>
              <div className="stat-icon">👥</div>
              <Card.Title>Total Users</Card.Title>
              <h3>{stats.totalUsers}</h3>
            </Card.Body>
          </Card>
        </Col>
        <Col md={3}>
          <Card className="stat-card h-100">
            <Card.Body>
              <div className="stat-icon">🏥</div>
              <Card.Title>Total Centers</Card.Title>
              <h3>{stats.totalCenters}</h3>
            </Card.Body>
          </Card>
        </Col>
        <Col md={3}>
          <Card className="stat-card h-100">
            <Card.Body>
              <div className="stat-icon">💉</div>
              <Card.Title>Total Drives</Card.Title>
              <h3>{stats.totalDrives}</h3>
            </Card.Body>
          </Card>
        </Col>
        <Col md={3}>
          <Card className="stat-card h-100">
            <Card.Body>
              <div className="stat-icon">⏳</div>
              <Card.Title>Pending Bookings</Card.Title>
              <h3 className="text-warning">{stats.totalBookings}</h3>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Search Bar */}
      <div className="mb-3">
        <Form.Control
          type="text"
          placeholder="🔍 Search..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="search-box"
          style={{ maxWidth: '300px' }}
        />
      </div>

      {/* Tab Navigation */}
      <Tabs activeKey={activeTab} onSelect={(k) => setActiveTab(k)} className="mb-3">
        <Tab eventKey="bookings" title={`📋 Pending Bookings (${bookings.length})`}>
          <Card>
            <Card.Body>
              {bookings.length === 0 ? (
                <Alert variant="info">No pending bookings</Alert>
              ) : (
                <div className="table-responsive">
                  <Table striped bordered hover responsive className="mb-0">
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
                      {filterBySearch(bookings).map((booking) => (
                        <tr key={booking.id}>
                          <td>{booking.id}</td>
                          <td>{booking.userName || 'N/A'}</td>
                          <td>{booking.driveName || booking.drive?.title}</td>
                          <td>{booking.centerName || booking.center?.name}</td>
                          <td>{booking.appointmentDate || booking.slotDate || 'N/A'}</td>
                          <td>
                            <span className={`badge bg-${
                              booking.status === 'PENDING' ? 'warning' : 
                              booking.status === 'APPROVED' ? 'success' : 'danger'
                            }`}>
                              {booking.status}
                            </span>
                          </td>
                          <td>
                            <Button variant="success" size="sm" className="me-2" onClick={() => handleApprove(booking.id)}>
                              ✅ Approve
                            </Button>
                            <Button variant="danger" size="sm" onClick={() => handleReject(booking.id)}>
                              ❌ Reject
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              )}
            </Card.Body>
          </Card>
        </Tab>

        <Tab eventKey="users" title={`👥 Users (${users.length})`}>
          <Card>
            <Card.Body>
              {users.length === 0 ? (
                <Alert variant="info">No users found</Alert>
              ) : (
                <div className="table-responsive">
                  <Table responsive className="mb-0">
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
                      {filterBySearch(users).map((user) => (
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
                            <Button variant="outline-danger" size="sm" onClick={() => handleDeleteUser(user.id)}>
                              Deactivate
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              )}
            </Card.Body>
          </Card>
        </Tab>

        <Tab eventKey="drives" title={`💉 Drives (${drives.length})`}>
          <Card>
            <Card.Body>
              {drives.length === 0 ? (
                <Alert variant="info">No drives found</Alert>
              ) : (
                <div className="table-responsive">
                  <Table responsive className="mb-0">
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
                      {filterBySearch(drives).map((drive) => (
                        <tr key={drive.id}>
                          <td>{drive.id}</td>
                          <td>{drive.title || drive.name}</td>
                          <td>{drive.vaccineName || 'N/A'}</td>
                          <td>{drive.center?.name || drive.centerName}</td>
                          <td>{drive.driveDate || drive.startDate}</td>
                          <td>{drive.availableSlots || 0}</td>
                          <td>
                            <Button variant="outline-danger" size="sm" onClick={() => handleDeleteDrive(drive.id)}>
                              🗑️ Delete
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              )}
            </Card.Body>
          </Card>
        </Tab>

        <Tab eventKey="centers" title={`🏥 Centers (${centers.length})`}>
          <Card>
            <Card.Body>
              {centers.length === 0 ? (
                <Alert variant="info">No centers found</Alert>
              ) : (
                <div className="table-responsive">
                  <Table responsive className="mb-0">
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
                      {filterBySearch(centers).map((center) => (
                        <tr key={center.id}>
                          <td>{center.id}</td>
                          <td>{center.name}</td>
                          <td>{center.city}</td>
                          <td>{center.address}</td>
                          <td>{center.phone || 'N/A'}</td>
                          <td>{center.dailyCapacity || center.capacityPerDay}</td>
                          <td>
                            <Button variant="outline-danger" size="sm" onClick={() => handleDeleteCenter(center.id)}>
                              🗑️ Delete
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </Table>
                </div>
              )}
            </Card.Body>
          </Card>
        </Tab>
      </Tabs>

      {/* Create Drive Modal */}
      <Modal show={showDriveModal} onHide={() => setShowDriveModal(false)} size="lg" centered>
        <Modal.Header closeButton>
          <Modal.Title>💉 Create New Drive</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateDrive}>
          <Modal.Body>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Drive Name</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={newDrive.name} 
                    onChange={(e) => setNewDrive({...newDrive, name: e.target.value})} 
                    required 
                    placeholder="e.g., COVID-19 Vaccination Drive"
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Vaccine Name</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={newDrive.vaccineName} 
                    onChange={(e) => setNewDrive({...newDrive, vaccineName: e.target.value})} 
                    required 
                    placeholder="e.g., Covishield"
                  />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control 
                as="textarea" 
                rows={2} 
                value={newDrive.description} 
                onChange={(e) => setNewDrive({...newDrive, description: e.target.value})} 
                placeholder="Drive description..."
              />
            </Form.Group>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Center</Form.Label>
                  <Form.Select 
                    value={newDrive.centerId} 
                    onChange={(e) => setNewDrive({...newDrive, centerId: e.target.value})} 
                    required
                  >
                    <option value="">Select Center</option>
                    {centers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>Min Age</Form.Label>
                  <Form.Control 
                    type="number" 
                    value={newDrive.minAge} 
                    onChange={(e) => setNewDrive({...newDrive, minAge: e.target.value})} 
                    required 
                  />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>Max Age</Form.Label>
                  <Form.Control 
                    type="number" 
                    value={newDrive.maxAge} 
                    onChange={(e) => setNewDrive({...newDrive, maxAge: e.target.value})} 
                    required 
                  />
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>Start Date</Form.Label>
                  <Form.Control 
                    type="date" 
                    value={newDrive.startDate} 
                    onChange={(e) => setNewDrive({...newDrive, startDate: e.target.value})} 
                    required 
                  />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>End Date</Form.Label>
                  <Form.Control 
                    type="date" 
                    value={newDrive.endDate} 
                    onChange={(e) => setNewDrive({...newDrive, endDate: e.target.value})} 
                    required 
                  />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>Start Time</Form.Label>
                  <Form.Control 
                    type="time" 
                    value={newDrive.startTime} 
                    onChange={(e) => setNewDrive({...newDrive, startTime: e.target.value})} 
                    required 
                  />
                </Form.Group>
              </Col>
              <Col md={3}>
                <Form.Group className="mb-3">
                  <Form.Label>End Time</Form.Label>
                  <Form.Control 
                    type="time" 
                    value={newDrive.endTime} 
                    onChange={(e) => setNewDrive({...newDrive, endTime: e.target.value})} 
                    required 
                  />
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
      <Modal show={showCenterModal} onHide={() => setShowCenterModal(false)} size="lg" centered>
        <Modal.Header closeButton>
          <Modal.Title>🏥 Create New Center</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateCenter}>
          <Modal.Body>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Center Name</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={newCenter.name} 
                    onChange={(e) => setNewCenter({...newCenter, name: e.target.value})} 
                    required 
                    placeholder="e.g., City Health Center"
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Email</Form.Label>
                  <Form.Control 
                    type="email" 
                    value={newCenter.email} 
                    onChange={(e) => setNewCenter({...newCenter, email: e.target.value})} 
                    required 
                    placeholder="contact@center.com"
                  />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3">
              <Form.Label>Address</Form.Label>
              <Form.Control 
                type="text" 
                value={newCenter.address} 
                onChange={(e) => setNewCenter({...newCenter, address: e.target.value})} 
                required 
                placeholder="Full address"
              />
            </Form.Group>
            <Row>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>City</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={newCenter.city} 
                    onChange={(e) => setNewCenter({...newCenter, city: e.target.value})} 
                    required 
                  />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>State</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={newCenter.state} 
                    onChange={(e) => setNewCenter({...newCenter, state: e.target.value})} 
                    required 
                  />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Pincode</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={newCenter.pincode} 
                    onChange={(e) => setNewCenter({...newCenter, pincode: e.target.value})} 
                    required 
                  />
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Phone</Form.Label>
                  <Form.Control 
                    type="text" 
                    value={newCenter.phone} 
                    onChange={(e) => setNewCenter({...newCenter, phone: e.target.value})} 
                    required 
                    placeholder="Phone number"
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Capacity Per Day</Form.Label>
                  <Form.Control 
                    type="number" 
                    value={newCenter.capacityPerDay} 
                    onChange={(e) => setNewCenter({...newCenter, capacityPerDay: e.target.value})} 
                    required 
                  />
                </Form.Group>
              </Col>
            </Row>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowCenterModal(false)}>Close</Button>
            <Button variant="success" type="submit">Create Center</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
};

export default AdminDashboard;
