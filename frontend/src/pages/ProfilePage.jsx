import React, { useState, useEffect } from 'react';
import { userAPI, unwrapApiData } from '../api/client';
import Skeleton from '../components/Skeleton';

export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState({ fullName: '', age: '' });
  const [passwordData, setPasswordData] = useState({ currentPassword: '', newPassword: '' });
  const [message, setMessage] = useState({ type: '', text: '' });

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const response = await userAPI.getProfile();
      const profileData = unwrapApiData(response) || {};
      setProfile(profileData);
      setFormData({ fullName: profileData.fullName || '', age: profileData.age || '' });
    } catch (err) {
      setError('Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      await userAPI.updateProfile({ fullName: formData.fullName, age: Number(formData.age) });
      setMessage({ type: 'success', text: 'Profile updated successfully' });
      setEditing(false);
      fetchProfile();
    } catch (err) {
      setMessage({ type: 'danger', text: 'Failed to update profile' });
    }
  };

  const handlePasswordChange = async (e) => {
    e.preventDefault();
    try {
      await userAPI.changePassword(passwordData);
      setMessage({ type: 'success', text: 'Password changed successfully' });
      setPasswordData({ currentPassword: '', newPassword: '' });
    } catch (err) {
      setMessage({ type: 'danger', text: 'Failed to change password' });
    }
  };

  if (loading) {
    return (
      <div className="container py-5">
        <Skeleton height="400px" />
      </div>
    );
  }

  return (
    <div className="container py-5">
      <div className="row">
        <div className="col-md-8 mx-auto">
          <div className="card shadow-sm">
            <div className="card-header bg-primary text-white">
              <h4 className="mb-0">My Profile</h4>
            </div>
            <div className="card-body">
              {message.text && (
                <div className={`alert alert-${message.type} alert-dismissible fade show`} role="alert">
                  {message.text}
                  <button type="button" className="btn-close" onClick={() => setMessage({ type: '', text: '' })}></button>
                </div>
              )}

              <div className="mb-4">
                <h5>Personal Information</h5>
                <hr />
                {editing ? (
                  <form onSubmit={handleUpdate}>
                    <div className="mb-3">
                      <label className="form-label">Full Name</label>
                      <input
                        type="text"
                        className="form-control"
                        value={formData.fullName}
                        onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                        required
                      />
                    </div>
                    <div className="mb-3">
                      <label className="form-label">Age</label>
                      <input
                        type="number"
                        className="form-control"
                        value={formData.age}
                        onChange={(e) => setFormData({ ...formData, age: e.target.value })}
                        required
                      />
                    </div>
                    <div className="mb-3">
                      <label className="form-label">Email</label>
                      <input type="email" className="form-control" value={profile?.email || ''} disabled />
                    </div>
                    <div className="d-flex gap-2">
                      <button type="submit" className="btn btn-primary">Save</button>
                      <button type="button" className="btn btn-secondary" onClick={() => setEditing(false)}>Cancel</button>
                    </div>
                  </form>
                ) : (
                  <div>
                    <p><strong>Name:</strong> {profile?.fullName}</p>
                    <p><strong>Email:</strong> {profile?.email}</p>
                    <p><strong>Age:</strong> {profile?.age}</p>
                    <p><strong>Email Verified:</strong> {profile?.emailVerified ? 'Yes' : 'No'}</p>
                    <button className="btn btn-primary" onClick={() => setEditing(true)}>Edit Profile</button>
                  </div>
                )}
              </div>

              <div className="mb-4">
                <h5>Change Password</h5>
                <hr />
                <form onSubmit={handlePasswordChange}>
                  <div className="mb-3">
                    <label className="form-label">Current Password</label>
                    <input
                      type="password"
                      className="form-control"
                      value={passwordData.currentPassword}
                      onChange={(e) => setPasswordData({ ...passwordData, currentPassword: e.target.value })}
                      required
                    />
                  </div>
                  <div className="mb-3">
                    <label className="form-label">New Password</label>
                    <input
                      type="password"
                      className="form-control"
                      value={passwordData.newPassword}
                      onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
                      required
                    />
                  </div>
                  <button type="submit" className="btn btn-warning">Change Password</button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
