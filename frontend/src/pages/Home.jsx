import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, Row, Col, Button, Container, Spinner, Alert } from 'react-bootstrap';
import { publicAPI } from '../api/axios';

const Home = () => {
  const [drives, setDrives] = useState([]);
  const [centers, setCenters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [drivesRes, centersRes] = await Promise.all([
          publicAPI.getDrives(),
          publicAPI.getCenters()
        ]);
        setDrives(drivesRes.data.slice(0, 3));
        setCenters(centersRes.data.slice(0, 3));
      } catch (err) {
        setError('Failed to load data. Please try again later.');
        console.error('Error fetching data:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

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
    <div className="home-page">
      {/* Hero Section */}
      <Container className="hero-section mb-5">
        <Row className="align-items-center">
          <Col md={8}>
            <h1 className="display-4 fw-bold mb-3">Welcome to Vaccination Scheduling System</h1>
            <p className="lead mb-4">Book your vaccination slot easily and stay protected. Our system makes it simple to find available drives, book appointments, and manage your vaccination records.</p>
            <div className="d-flex gap-3">
              <Link to="/register">
                <Button variant="light" size="lg" className="btn-hover">
                  🚀 Get Started
                </Button>
              </Link>
              <Link to="/drives">
                <Button variant="outline-light" size="lg" className="btn-hover">
                  📅 View Drives
                </Button>
              </Link>
            </div>
          </Col>
          <Col md={4} className="text-center">
            <div className="hero-icon">
              <span style={{ fontSize: '6rem' }}>💉</span>
            </div>
          </Col>
        </Row>
      </Container>

      {error && (
        <Alert variant="danger" className="mb-4">
          {error}
        </Alert>
      )}

      {/* Upcoming Drives Section */}
      <section className="mb-5">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h2 className="mb-0">🏥 Upcoming Vaccination Drives</h2>
          <Link to="/drives">
            <Button variant="outline-primary" size="sm">View All →</Button>
          </Link>
        </div>
        {drives.length === 0 ? (
          <Alert variant="info">No upcoming drives at the moment.</Alert>
        ) : (
          <Row>
            {drives.map((drive, index) => (
              <Col md={4} key={drive.id} className="mb-3" style={{ animationDelay: `${index * 0.1}s` }}>
                <Card className="h-100 drive-card">
                  <Card.Header className="d-flex justify-content-between align-items-center">
                    <span className="badge bg-primary">{drive.vaccineName}</span>
                    <small className="text-muted">{drive.vaccineManufacturer}</small>
                  </Card.Header>
                  <Card.Body>
                    <Card.Title className="drive-title">{drive.name}</Card.Title>
                    <Card.Subtitle className="mb-2 text-muted">{drive.centerName}</Card.Subtitle>
                    <Card.Text className="drive-description">
                      {drive.description?.substring(0, 100)}...
                    </Card.Text>
                    <div className="drive-info">
                      <p className="mb-1"><strong>📅 Dates:</strong> {drive.startDate} - {drive.endDate}</p>
                      <p className="mb-1"><strong>⏰ Time:</strong> {drive.startTime} - {drive.endTime}</p>
                      <p className="mb-0"><strong>👥 Age:</strong> {drive.minAge} - {drive.maxAge} years</p>
                    </div>
                  </Card.Body>
                  <Card.Footer>
                    <div className="d-flex justify-content-between align-items-center">
                      <span className="text-success fw-bold">💉 {drive.availableSlots} slots available</span>
                      <Link to={`/drives`}>
                        <Button variant="outline-primary" size="sm">View Details</Button>
                      </Link>
                    </div>
                  </Card.Footer>
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </section>

      {/* Vaccination Centers Section */}
      <section className="mb-5">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h2 className="mb-0">🏨 Vaccination Centers</h2>
          <Link to="/centers">
            <Button variant="outline-primary" size="sm">View All →</Button>
          </Link>
        </div>
        {centers.length === 0 ? (
          <Alert variant="info">No centers available at the moment.</Alert>
        ) : (
          <Row>
            {centers.map((center, index) => (
              <Col md={4} key={center.id} className="mb-3" style={{ animationDelay: `${index * 0.1}s` }}>
                <Card className="h-100 center-card">
                  <Card.Header className="d-flex justify-content-between align-items-center">
                    <span className="badge bg-success">Active</span>
                    <small className="text-muted">{center.city}</small>
                  </Card.Header>
                  <Card.Body>
                    <Card.Title>{center.name}</Card.Title>
                    <Card.Text>
                      <p className="mb-1">📍 {center.address}</p>
                      <p className="mb-1">📞 {center.phone}</p>
                      <p className="mb-0">📧 {center.email}</p>
                    </Card.Text>
                  </Card.Body>
                  <Card.Footer>
                    <div className="d-flex justify-content-between align-items-center">
                      <span className="text-info fw-bold">Capacity: {center.capacityPerDay}/day</span>
                      <Link to={`/centers`}>
                        <Button variant="outline-primary" size="sm">View Details</Button>
                      </Link>
                    </div>
                  </Card.Footer>
                </Card>
              </Col>
            ))}
          </Row>
        )}
      </section>

      {/* Features Section */}
      <section className="features-section mb-5">
        <h2 className="text-center mb-4">✨ Why Choose Us?</h2>
        <Row className="g-4">
          <Col md={4}>
            <Card className="feature-card text-center h-100">
              <Card.Body>
                <div className="feature-icon mb-3">⚡</div>
                <Card.Title>Easy Booking</Card.Title>
                <Card.Text>Book your slot in just a few clicks with our intuitive interface</Card.Text>
              </Card.Body>
            </Card>
          </Col>
          <Col md={4}>
            <Card className="feature-card text-center h-100">
              <Card.Body>
                <div className="feature-icon mb-3">🔔</div>
                <Card.Title>Real-time Updates</Card.Title>
                <Card.Text>Get instant notifications about your booking status and changes</Card.Text>
              </Card.Body>
            </Card>
          </Col>
          <Col md={4}>
            <Card className="feature-card text-center h-100">
              <Card.Body>
                <div className="feature-icon mb-3">🔒</div>
                <Card.Title>Secure & Private</Card.Title>
                <Card.Text>Your data is protected with industry-standard security</Card.Text>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </section>

      {/* Stats Section */}
      <section className="stats-section mb-5">
        <Row className="text-center">
          <Col md={3}>
            <div className="stat-item">
              <h2 className="display-4 fw-bold text-primary">5+</h2>
              <p className="text-muted">Vaccination Centers</p>
            </div>
          </Col>
          <Col md={3}>
            <div className="stat-item">
              <h2 className="display-4 fw-bold text-success">5+</h2>
              <p className="text-muted">Active Drives</p>
            </div>
          </Col>
          <Col md={3}>
            <div className="stat-item">
              <h2 className="display-4 fw-bold text-info">20K+</h2>
              <p className="text-muted">Slots Available</p>
            </div>
          </Col>
          <Col md={3}>
            <div className="stat-item">
              <h2 className="display-4 fw-bold text-warning">24/7</h2>
              <p className="text-muted">Support Available</p>
            </div>
          </Col>
        </Row>
      </section>

      {/* CTA Section */}
      <Container className="text-center mb-5">
        <Card className="cta-card">
          <Card.Body className="py-5">
            <h2 className="mb-3">Ready to Get Vaccinated?</h2>
            <p className="mb-4">Join thousands of people who have already booked their vaccination slots through our system.</p>
            <Link to="/register">
              <Button variant="primary" size="lg" className="btn-hover">
                Register Now - It's Free! 🎉
              </Button>
            </Link>
          </Card.Body>
        </Card>
      </Container>
    </div>
  );
};

export default Home;
