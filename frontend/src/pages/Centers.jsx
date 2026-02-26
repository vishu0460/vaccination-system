import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Form, Spinner, Alert, InputGroup, Button } from 'react-bootstrap';
import { publicAPI } from '../api/axios';

const Centers = () => {
  const [centers, setCenters] = useState([]);
  const [filteredCenters, setFilteredCenters] = useState([]);
  const [cityFilter, setCityFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchCenters = async () => {
      try {
        setLoading(true);
        const res = await publicAPI.getCenters();
        setCenters(res.data);
        setFilteredCenters(res.data);
      } catch (err) {
        setError('Failed to load centers. Please try again.');
        console.error('Error fetching centers:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchCenters();
  }, []);

  useEffect(() => {
    if (cityFilter) {
      setFilteredCenters(centers.filter(c => 
        c.city.toLowerCase().includes(cityFilter.toLowerCase()) ||
        c.name.toLowerCase().includes(cityFilter.toLowerCase()) ||
        c.state.toLowerCase().includes(cityFilter.toLowerCase())
      ));
    } else {
      setFilteredCenters(centers);
    }
  }, [cityFilter, centers]);

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
    <div className="centers-page">
      <h2 className="mb-4">🏥 Vaccination Centers</h2>
      
      {error && (
        <Alert variant="danger" className="mb-4">
          {error}
        </Alert>
      )}
      
      <Form.Group className="mb-4">
        <InputGroup>
          <InputGroup.Text>🔍</InputGroup.Text>
          <Form.Control
            type="text"
            placeholder="Search by city, center name, or state..."
            value={cityFilter}
            onChange={(e) => setCityFilter(e.target.value)}
          />
          {cityFilter && (
            <Button 
              variant="outline-secondary" 
              onClick={() => setCityFilter('')}
            >
              Clear
            </Button>
          )}
        </InputGroup>
      </Form.Group>
      
      <p className="text-muted mb-3">
        Showing {filteredCenters.length} of {centers.length} centers
      </p>
      
      {filteredCenters.length === 0 ? (
        <Alert variant="info">No centers found matching your search.</Alert>
      ) : (
        <Row>
          {filteredCenters.map((center, index) => (
            <Col md={6} lg={4} key={center.id} className="mb-4" style={{ animationDelay: `${index * 0.1}s` }}>
              <Card className="h-100 center-card">
                <Card.Header className="d-flex justify-content-between align-items-center">
                  <span className="badge bg-success">Active</span>
                  <small className="text-muted">{center.city}, {center.state}</small>
                </Card.Header>
                <Card.Body>
                  <Card.Title>{center.name}</Card.Title>
                  <Card.Text>
                    <p className="mb-2">📍 {center.address}</p>
                    <p className="mb-2">📞 {center.phone}</p>
                    <p className="mb-2">📧 {center.email}</p>
                    <p className="mb-0">📌 {center.state} - {center.pincode}</p>
                  </Card.Text>
                </Card.Body>
                <Card.Footer>
                  <div className="d-flex justify-content-between align-items-center">
                    <span className="text-info fw-bold">
                      Capacity: {center.capacityPerDay}/day
                    </span>
                  </div>
                </Card.Footer>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  );
};

export default Centers;
