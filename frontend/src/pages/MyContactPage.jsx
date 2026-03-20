import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Badge, Spinner, Alert, Button, Form } from 'react-bootstrap';
import { contactAPI } from '../api/client';

export default function MyContactPage() {
  const [contacts, setContacts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    subject: '',
    message: ''
  });
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState('');

  useEffect(() => {
    fetchMyContacts();
  }, []);

  const fetchMyContacts = async () => {
    try {
      setLoading(true);
      const response = await contactAPI.getMyInquiries();
      setContacts(response.data);
    } catch (err) {
      setError('Failed to load your inquiries.');
      console.error('Error fetching contacts:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    
    try {
      await contactAPI.submitContact(formData);
      setSuccess('Your inquiry has been submitted successfully!');
      setShowForm(false);
      setFormData({ name: '', email: '', subject: '', message: '' });
      fetchMyContacts();
    } catch (err) {
      setError('Failed to submit inquiry. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusBadge = (status) => {
    const colors = {
      PENDING: 'warning',
      REPLIED: 'success',
      CLOSED: 'secondary'
    };
    return <Badge bg={colors[status] || 'secondary'}>{status}</Badge>;
  };

  if (loading) {
    return (
      <Container className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
        <p className="mt-3">Loading your inquiries...</p>
      </Container>
    );
  }

  return (
    <Container className="py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>My Contact Inquiries</h2>
        <Button variant="primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : 'New Inquiry'}
        </Button>
      </div>
      
      {success && <Alert variant="success" dismissible onClose={() => setSuccess('')}>{success}</Alert>}
      {error && <Alert variant="danger">{error}</Alert>}
      
      {showForm && (
        <Card className="mb-4">
          <Card.Header>Submit New Inquiry</Card.Header>
          <Card.Body>
            <Form onSubmit={handleSubmit}>
              <Row>
                <Col md={6}>
                  <Form.Group className="mb-3">
                    <Form.Label>Name</Form.Label>
                    <Form.Control
                      type="text"
                      value={formData.name}
                      onChange={(e) => setFormData({...formData, name: e.target.value})}
                      required
                    />
                  </Form.Group>
                </Col>
                <Col md={6}>
                  <Form.Group className="mb-3">
                    <Form.Label>Email</Form.Label>
                    <Form.Control
                      type="email"
                      value={formData.email}
                      onChange={(e) => setFormData({...formData, email: e.target.value})}
                      required
                    />
                  </Form.Group>
                </Col>
              </Row>
              <Form.Group className="mb-3">
                <Form.Label>Subject</Form.Label>
                <Form.Control
                  type="text"
                  value={formData.subject}
                  onChange={(e) => setFormData({...formData, subject: e.target.value})}
                  required
                />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Message</Form.Label>
                <Form.Control
                  as="textarea"
                  rows={4}
                  value={formData.message}
                  onChange={(e) => setFormData({...formData, message: e.target.value})}
                  required
                />
              </Form.Group>
              <Button type="submit" variant="primary" disabled={submitting}>
                {submitting ? 'Submitting...' : 'Submit Inquiry'}
              </Button>
            </Form>
          </Card.Body>
        </Card>
      )}
      
      {contacts.length === 0 && !showForm ? (
        <Card>
          <Card.Body className="text-center py-5">
            <h4>No Inquiries Yet</h4>
            <p className="text-muted">You haven't submitted any contact inquiries.</p>
          </Card.Body>
        </Card>
      ) : (
        <Row>
          {contacts.map((item) => (
            <Col key={item.id} md={6} className="mb-3">
              <Card className="h-100">
                <Card.Header className="d-flex justify-content-between align-items-center">
                  <span>{item.subject}</span>
                  {getStatusBadge(item.status)}
                </Card.Header>
                <Card.Body>
                  <Card.Text>{item.message}</Card.Text>
                  
                  {(item.replyMessage || item.response) && (
                    <div className="mt-3 p-3 bg-light rounded">
                      <h6 className="text-success">Admin Reply:</h6>
                      <p className="mb-0">{item.replyMessage || item.response}</p>
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
