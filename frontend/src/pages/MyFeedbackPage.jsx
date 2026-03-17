import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Badge, Spinner, Alert, Button } from 'react-bootstrap';
import { apiClient } from '../api/client';

export default function MyFeedbackPage() {
  const [feedback, setFeedback] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchMyFeedback();
  }, []);

  const fetchMyFeedback = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get('/feedback/my-feedback');
      setFeedback(response.data);
    } catch (err) {
      setError('Failed to load feedback. Please try again.');
      console.error('Error fetching feedback:', err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    const colors = {
      PENDING: 'warning',
      APPROVED: 'success',  // Feedback responded/approved by admin
      REJECTED: 'danger'
    };
    return <Badge bg={colors[status] || 'secondary'}>{status || 'PENDING'}</Badge>;
  };

  if (loading) {
    return (
      <Container className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
        <p className="mt-3">Loading your feedback...</p>
      </Container>
    );
  }

  return (
    <Container className="py-4">
      <h2 className="mb-4">My Feedback & Responses</h2>
      
      {error && <Alert variant="danger">{error}</Alert>}
      
      {feedback.length === 0 ? (
        <Card>
          <Card.Body className="text-center py-5">
            <h4>No Feedback Submitted</h4>
            <p className="text-muted">You haven't submitted any feedback yet.</p>
            <Button variant="primary" href="/feedback">Submit Feedback</Button>
          </Card.Body>
        </Card>
      ) : (
        <Row>
          {feedback.map((item) => (
            <Col key={item.id} md={6} className="mb-3">
              <Card className="h-100">
                <Card.Header className="d-flex justify-content-between align-items-center">
                  <span>Feedback #{item.id}</span>
                  {getStatusBadge(item.status)}
                </Card.Header>
                <Card.Body>
                  <Card.Title>{item.subject}</Card.Title>
                  <Card.Text>{item.message}</Card.Text>
                  
                  {item.response && (
                    <div className="mt-3 p-3 bg-light rounded">
                      <h6 className="text-success">Admin Response:</h6>
                      <p className="mb-0">{item.response}</p>
                    </div>
                  )}
                  
                  <small className="text-muted d-block mt-2">
                    Submitted: {new Date(item.createdAt).toLocaleDateString()}
                  </small>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </Container>
  );
}
