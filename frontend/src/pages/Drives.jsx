import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Spinner, Alert } from 'react-bootstrap';
import { publicAPI } from '../api/axios';
import { useToast } from '../components/Toast';
import { SkeletonCard } from '../components/Skeleton';

const Drives = () => {
  const toast = useToast();
  const [drives, setDrives] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

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

  // Filter drives by search term
  const filteredDrives = drives.filter(drive => {
    if (!searchTerm) return true;
    const search = searchTerm.toLowerCase();
    return (
      (drive.name || drive.title || '').toLowerCase().includes(search) ||
      (drive.vaccineName || '').toLowerCase().includes(search) ||
      (drive.centerName || drive.center?.name || '').toLowerCase().includes(search) ||
      (drive.city || '').toLowerCase().includes(search)
    );
  });

  if (loading) {
    return (
      <div className="drives-page fade-in">
        <h2 className="mb-4">💉 Vaccination Drives</h2>
        <Row>
          {[1, 2, 3, 4, 5, 6].map(i => (
            <Col md={6} lg={4} key={i} className="mb-4">
              <SkeletonCard height="280px" />
            </Col>
          ))}
        </Row>
      </div>
    );
  }

  return (
    <div className="drives-page fade-in">
      <div className="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-3">
        <h2 className="mb-0">💉 Vaccination Drives</h2>
        <div className="search-box">
          <input
            type="text"
            className="form-control"
            placeholder="🔍 Search drives..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ width: '250px' }}
          />
        </div>
      </div>
      
      {error && (
        <Alert variant="danger" className="mb-4">
          {error}
        </Alert>
      )}
      
      {filteredDrives.length === 0 ? (
        <Alert variant="info">
          {searchTerm ? 'No drives match your search criteria.' : 'No vaccination drives available at the moment.'}
        </Alert>
      ) : (
        <Row>
          {filteredDrives.map((drive, index) => (
            <Col md={6} lg={4} key={drive.id} className="mb-4" style={{ animationDelay: `${index * 0.05}s` }}>
              <Card className="h-100 drive-card hover-lift">
                <Card.Header className="d-flex justify-content-between align-items-center">
                  <span className="badge bg-primary">{drive.vaccineName || 'Vaccine'}</span>
                  <small className="text-muted">{drive.city || 'Metropolis'}</small>
                </Card.Header>
                <Card.Body>
                  <Card.Title>{drive.title || drive.name}</Card.Title>
                  <Card.Subtitle className="mb-3 text-muted">
                    📍 {drive.center?.name || drive.centerName}
                  </Card.Subtitle>
                  <Card.Text>
                    {drive.description && (
                      <p className="mb-3">{drive.description}</p>
                    )}
                    <div className="drive-details">
                      <p className="mb-1">📅 <strong>Date:</strong> {drive.driveDate || drive.startDate}</p>
                      <p className="mb-1">⏰ <strong>Time:</strong> {drive.startTime || '9:00 AM'} - {drive.endTime || '5:00 PM'}</p>
                      <p className="mb-1">👥 <strong>Age:</strong> {drive.minAge || 18} - {drive.maxAge || 60} years</p>
                      {drive.dosesRequired && (
                        <p className="mb-0">💉 <strong>Doses:</strong> {drive.dosesRequired} dose(s)</p>
                      )}
                    </div>
                  </Card.Text>
                </Card.Body>
                <Card.Footer>
                  <div className="d-flex justify-content-between align-items-center">
                    <span className={drive.availableSlots > 0 ? 'text-success fw-bold' : 'text-danger fw-bold'}>
                      💉 {drive.availableSlots || 0} slots available
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
