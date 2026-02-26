import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { publicAPI } from '../api/axios';

const Drives = () => {
  const [drives, setDrives] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchDrives = async () => {
      try {
        setLoading(true);
        const res = await publicAPI.getDrives();
        setDrives(res.data);
      } catch (err) {
        setError('Failed to load drives. Please try again.');
        console.error('Error fetching drives:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchDrives();
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
    <div className="drives-page">
      <h2 className="mb-4">💉 Vaccination Drives</h2>
      
      {error && (
        <Alert variant="danger" className="mb-4">
          {error}
        </Alert>
      )}
      
      {drives.length === 0 ? (
        <Alert variant="info">No vaccination drives available at the moment.</Alert>
      ) : (
        <Row>
          {drives.map((drive, index) => (
            <Col md={6} lg={4} key={drive.id} className="mb-4" style={{ animationDelay: `${index * 0.1}s` }}>
              <Card className="h-100 drive-card">
                <Card.Header className="d-flex justify-content-between align-items-center">
                  <span className="badge bg-primary">{drive.vaccineName}</span>
                  <small className="text-muted">{drive.vaccineManufacturer}</small>
                </Card.Header>
                <Card.Body>
                  <Card.Title>{drive.name}</Card.Title>
                  <Card.Subtitle className="mb-3 text-muted">
                    📍 {drive.centerName}
                  </Card.Subtitle>
                  <Card.Text>
                    {drive.description && (
                      <p className="mb-3">{drive.description}</p>
                    )}
                    <div className="drive-details">
                      <p className="mb-1">📅 <strong>Dates:</strong> {drive.startDate} to {drive.endDate}</p>
                      <p className="mb-1">⏰ <strong>Time:</strong> {drive.startTime} - {drive.endTime}</p>
                      <p className="mb-1">👥 <strong>Age:</strong> {drive.minAge} - {drive.maxAge} years</p>
                      <p className="mb-1">💉 <strong>Doses:</strong> {drive.dosesRequired} dose(s)</p>
                      {drive.doseGapDays > 0 && (
                        <p className="mb-0">📆 <strong>Gap between doses:</strong> {drive.doseGapDays} days</p>
                      )}
                    </div>
                  </Card.Text>
                </Card.Body>
                <Card.Footer>
                  <div className="d-flex justify-content-between align-items-center">
                    <span className={drive.availableSlots > 0 ? 'text-success fw-bold' : 'text-danger fw-bold'}>
                      💉 {drive.availableSlots} slots available
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

export default Drives;
