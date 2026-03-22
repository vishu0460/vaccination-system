import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Table, Badge, Button, Spinner, Modal, Form, Alert, Tab, Tabs } from 'react-bootstrap';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, ArcElement } from 'chart.js';
import { Bar, Doughnut } from 'react-chartjs-2';
import { adminAPI, newsAPI, certificateAPI, superAdminAPI, unwrapApiData } from '../api/client';
import ErrorState from '../components/ErrorState';
import { getRole } from '../utils/auth';
import { FaUsers, FaCalendarCheck, FaSyringe, FaHospital, FaNewspaper, FaCertificate, FaSearch, FaPlus, FaTrash, FaEdit, FaCheck, FaTimes, FaDownload, FaChartLine, FaEnvelope, FaBell, FaCog, FaUserShield, FaComment, FaPhone } from 'react-icons/fa';
import useDebounce from '../hooks/useDebounce';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, ArcElement);

const EMPTY_STATS = {
  totalUsers: 0,
  totalBookings: 0,
  activeDrives: 0,
  totalCenters: 0,
  newUsersThisMonth: 0,
  bookingsToday: 0,
  completedVaccinations: 0
};

const DRIVE_STATUS_OPTIONS = ['UPCOMING', 'LIVE', 'EXPIRED'];

const ensureArray = (payload) => {
  if (Array.isArray(payload)) {
    return payload;
  }

  if (Array.isArray(payload?.content)) {
    return payload.content;
  }

  if (Array.isArray(payload?.data)) {
    return payload.data;
  }

  return [];
};

const ensureStats = (payload) => {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return EMPTY_STATS;
  }

  return { ...EMPTY_STATS, ...payload };
};

const formatDate = (value) => {
  if (!value) {
    return 'N/A';
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? 'N/A' : parsed.toLocaleDateString();
};

const formatDateTime = (value) => {
  if (!value) {
    return 'N/A';
  }

  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? 'N/A' : parsed.toLocaleString();
};

const getSlotStatusPreview = (startValue, endValue) => {
  const start = new Date(startValue);
  const end = new Date(endValue);
  const now = new Date();

  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
    return null;
  }

  if (end <= start || now > end) {
    return 'EXPIRED';
  }

  if (now >= start && now <= end) {
    return 'LIVE';
  }

  return 'UPCOMING';
};

const formatDateTimeLocal = (value) => {
  if (!value) {
    return '';
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return '';
  }

  const pad = (part) => String(part).padStart(2, '0');
  return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())}T${pad(parsed.getHours())}:${pad(parsed.getMinutes())}`;
};

const formatApiDateTime = (value) => {
  if (!value) {
    return '';
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return '';
  }

  const pad = (part) => String(part).padStart(2, '0');
  return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())}T${pad(parsed.getHours())}:${pad(parsed.getMinutes())}:${pad(parsed.getSeconds())}`;
};

const isDateTimeValue = (value) => typeof value === 'string' && value.includes('T');

const combineSlotDateTime = (baseDateTime, timeValue) => {
  if (!timeValue && !baseDateTime) {
    return '';
  }

  if (isDateTimeValue(timeValue)) {
    return formatDateTimeLocal(timeValue);
  }

  const parsed = new Date(baseDateTime);
  if (Number.isNaN(parsed.getTime())) {
    return '';
  }

  const [hours = '0', minutes = '0'] = String(timeValue || '').split(':');
  const parsedHours = Number(hours);
  const parsedMinutes = Number(minutes);

  if (Number.isNaN(parsedHours) || Number.isNaN(parsedMinutes)) {
    return '';
  }

  parsed.setHours(parsedHours, parsedMinutes, 0, 0);
  return formatDateTimeLocal(parsed);
};

const normalizeSlotForEditing = (slot) => {
  const baseStart = slot?.startDate || slot?.dateTime || slot?.time || slot?.startTime || '';
  const rawEnd = slot?.endDate || slot?.endTime || slot?.dateEndTime || slot?.endDateTime || '';
  const normalizedDriveId = slot?.driveId || slot?.drive?.id || '';
  const normalizedStartTime = formatDateTimeLocal(baseStart);
  let normalizedEndTime = formatDateTimeLocal(rawEnd) || combineSlotDateTime(baseStart, rawEnd);

  if (!normalizedEndTime && normalizedStartTime) {
    const fallbackEnd = new Date(normalizedStartTime);
    if (!Number.isNaN(fallbackEnd.getTime())) {
      fallbackEnd.setHours(fallbackEnd.getHours() + 1);
      normalizedEndTime = formatDateTimeLocal(fallbackEnd);
    }
  }

  return {
    ...slot,
    startDate: slot?.startDate || baseStart || null,
    endDate: slot?.endDate || rawEnd || null,
    driveId: normalizedDriveId,
    editStartTime: slot?.editStartTime || normalizedStartTime,
    editEndTime: slot?.editEndTime || normalizedEndTime,
  };
};

const formatSlotEndDisplay = (slot) => {
  const endDateTime = slot?.endDate || combineSlotDateTime(slot?.startDate || slot?.dateTime || slot?.time || slot?.startTime, slot?.endTime);
  return formatDateTime(endDateTime);
};

const buildSlotPayload = (formState) => {
  const startDate = formatApiDateTime(formState.startTime) || formState.startTime;
  const endDate = formatApiDateTime(formState.endTime) || formState.endTime;

  return {
    driveId: Number(formState.driveId),
    startDate,
    endDate,
    capacity: Number(formState.capacity)
  };
};

const mergeUpdatedSlot = (currentSlots, updatedSlot) =>
  currentSlots.map((slot) => (Number(slot.id) === Number(updatedSlot.id) ? normalizeSlotForEditing(updatedSlot) : slot));

const notifyDataUpdated = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('vaxzone:data-updated'));
  }
};

const communicationBadge = (status) => {
  const normalized = String(status || 'PENDING').toUpperCase();
  return <Badge bg={normalized === 'REPLIED' ? 'success' : 'warning'}>{normalized}</Badge>;
};

export default function AdminDashboardPage() {
  const currentRole = getRole();
  const isSuperAdmin = currentRole === 'SUPER_ADMIN';
  const [activeTab, setActiveTab] = useState('dashboard');
  const [stats, setStats] = useState(EMPTY_STATS);
  const [users, setUsers] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [centers, setCenters] = useState([]);
  const [drives, setDrives] = useState([]);
  const [slots, setSlots] = useState([]);
  const [news, setNews] = useState([]);
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearchTerm = useDebounce(searchTerm, 250);
  
  // Modal states
  const [showNewsModal, setShowNewsModal] = useState(false);
  const [showEditNewsModal, setShowEditNewsModal] = useState(false);
  const [editingNews, setEditingNews] = useState(null);
  const [editingNewsForm, setEditingNewsForm] = useState({});

  const [showCenterModal, setShowCenterModal] = useState(false);
  const [showEditCenterModal, setShowEditCenterModal] = useState(false);
  const [showDriveModal, setShowDriveModal] = useState(false);
  const [showEditDriveModal, setShowEditDriveModal] = useState(false);
  const [showSlotModal, setShowSlotModal] = useState(false);
  const [showManageSlotsModal, setShowManageSlotsModal] = useState(false);
  const [showEditSlotModal, setShowEditSlotModal] = useState(false);
  const [showCertificateModal, setShowCertificateModal] = useState(false);
  const [showEditBookingModal, setShowEditBookingModal] = useState(false);
  const [showEditUserModal, setShowEditUserModal] = useState(false);
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [selectedDrive, setSelectedDrive] = useState(null);
  const [editingCenter, setEditingCenter] = useState(null);
  const [editingDrive, setEditingDrive] = useState(null);
  const [editingSlot, setEditingSlot] = useState(null);
  const [editingUser, setEditingUser] = useState(null);
  const [driveSlots, setDriveSlots] = useState([]);
  
  // Form states
  const [newsForm, setNewsForm] = useState({ title: '', content: '', category: 'GENERAL' });
  const [editNewsForm, setEditNewsForm] = useState({ title: '', content: '', category: 'GENERAL' });

  const [centerForm, setCenterForm] = useState({ name: '', address: '', city: '', state: '', pincode: '', phone: '', email: '', workingHours: '', dailyCapacity: 100 });
  const [editCenterForm, setEditCenterForm] = useState({ name: '', address: '', city: '', state: '', pincode: '', phone: '', email: '', workingHours: '', dailyCapacity: 100 });
  const [driveForm, setDriveForm] = useState({ title: '', description: '', vaccineType: '', centerId: '', driveDate: '', minAge: 18, maxAge: 100, totalSlots: 100, status: 'UPCOMING' });
  const [editDriveForm, setEditDriveForm] = useState({ title: '', description: '', vaccineType: '', centerId: '', driveDate: '', minAge: 18, maxAge: 100, totalSlots: 100, status: 'UPCOMING' });
  const [slotForm, setSlotForm] = useState({ driveId: '', startTime: '', endTime: '', capacity: 50 });
  const [editSlotForm, setEditSlotForm] = useState({ driveId: '', startTime: '', endTime: '', capacity: 50 });
  const [slotActionLoading, setSlotActionLoading] = useState(false);
  const [certificateForm, setCertificateForm] = useState({ bookingId: '', vaccineName: '', doseNumber: 1 });
  const [editUserForm, setEditUserForm] = useState({ email: '', fullName: '', age: 0, phoneNumber: '', enabled: true });
  const [slotFilters, setSlotFilters] = useState({ status: 'ALL', centerId: '', driveId: '', date: '' });
  
  // Feedback and Contact states
  const [feedbacks, setFeedbacks] = useState([]);
  const [contacts, setContacts] = useState([]);
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const [showContactModal, setShowContactModal] = useState(false);
  const [selectedFeedback, setSelectedFeedback] = useState(null);
  const [selectedContact, setSelectedContact] = useState(null);
  const [responseText, setResponseText] = useState('');

  const refreshActiveTabData = async () => {
    await loadDashboardData();

    if (activeTab === 'users') await loadUsers();
    if (activeTab === 'bookings') await loadBookings();
    if (activeTab === 'centers') await loadCenters();
    if (activeTab === 'drives') await loadDrives();
    if (activeTab === 'slots') await loadSlots();
    if (activeTab === 'news') await loadNews();
    if (activeTab === 'certificates') await loadCertificates();
    if (activeTab === 'feedback') await loadFeedbacks();
    if (activeTab === 'contacts') await loadContacts();

    if (selectedDrive?.id) {
      await loadDriveSlots(selectedDrive.id, showManageSlotsModal);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  useEffect(() => {
    const handleDataUpdated = () => {
      refreshActiveTabData().catch((err) => {
        setError(err?.response?.data?.message || 'Failed to refresh dashboard data');
      });
    };

    window.addEventListener('vaxzone:data-updated', handleDataUpdated);
    return () => window.removeEventListener('vaxzone:data-updated', handleDataUpdated);
  }, [activeTab, selectedDrive?.id, showManageSlotsModal, slotFilters]);

  useEffect(() => {
    if (activeTab === 'users') loadUsers();
    if (activeTab === 'bookings') loadBookings();
    if (activeTab === 'centers') loadCenters();
    if (activeTab === 'drives') loadDrives();
    if (activeTab === 'slots') loadSlots();
    if (activeTab === 'news') loadNews();
    if (activeTab === 'certificates') loadCertificates();
    if (activeTab === 'feedback') loadFeedbacks();
    if (activeTab === 'contacts') loadContacts();
  }, [activeTab]);

  useEffect(() => {
    if (activeTab === 'slots') {
      loadSlots();
    }
  }, [slotFilters]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [statsRes, bookingsRes] = await Promise.all([
        adminAPI.getDashboardStats(),
        adminAPI.getAllBookings()
      ]);
      setStats(ensureStats(unwrapApiData(statsRes)));
      setBookings(ensureArray(unwrapApiData(bookingsRes)));
    } catch (err) {
      console.error('Error loading dashboard:', err.response?.data || err.message);
      setStats(EMPTY_STATS);
      setBookings([]);
      setError(err.response?.data?.message || 'Failed to load dashboard stats');
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getAllUsers();
      const payload = unwrapApiData(response) || {};
      const items = ensureArray(payload);
      setUsers(items);
      setTotalPages(Math.ceil((payload.totalElements || items.length) / 10));
    } catch (err) {
      setError('Failed to load users');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const loadBookings = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getAllBookings();
      setBookings(ensureArray(unwrapApiData(response)));
    } catch (err) {
      setError('Failed to load bookings');
      setBookings([]);
    } finally {
      setLoading(false);
    }
  };

  const loadCenters = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getAllCenters();
      setCenters(ensureArray(unwrapApiData(response)));
    } catch (err) {
      setError('Failed to load centers');
      setCenters([]);
    } finally {
      setLoading(false);
    }
  };

  const loadDrives = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getAllDrives();
      setDrives(ensureArray(unwrapApiData(response)));
    } catch (err) {
      setError('Failed to load drives');
      setDrives([]);
    } finally {
      setLoading(false);
    }
  };

  const buildSlotFilterParams = () => {
    const params = {};

    if (slotFilters.status && slotFilters.status !== 'ALL') {
      params.status = slotFilters.status;
    }
    if (slotFilters.centerId) {
      params.centerId = slotFilters.centerId;
    }
    if (slotFilters.driveId) {
      params.driveId = slotFilters.driveId;
    }
    if (slotFilters.date) {
      params.date = slotFilters.date;
    }

    return params;
  };

  const loadSlots = async () => {
    try {
      setLoading(true);
      setError(null);

      const slotsResponse = await adminAPI.getAllSlotsList(buildSlotFilterParams());
      setSlots(ensureArray(unwrapApiData(slotsResponse)).map(normalizeSlotForEditing));

      const metadataRequests = [];
      if (centers.length === 0) {
        metadataRequests.push(
          adminAPI.getAllCenters()
            .then((response) => setCenters(ensureArray(unwrapApiData(response))))
            .catch(() => {})
        );
      }
      if (drives.length === 0) {
        metadataRequests.push(
          adminAPI.getAllDrives()
            .then((response) => setDrives(ensureArray(unwrapApiData(response))))
            .catch(() => {})
        );
      }

      await Promise.all(metadataRequests);
    } catch (err) {
      setError('Failed to load slots');
      setSlots([]);
    } finally {
      setLoading(false);
    }
  };

  const loadNews = async () => {
    try {
      setLoading(true);
      const response = await newsAPI.getAdminNews(0, 100);
      const payload = unwrapApiData(response) || {};
      setNews(ensureArray(payload));
    } catch (err) {
      setNews([]);
    } finally {
      setLoading(false);
    }
  };

  const loadCertificates = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getAllCertificates();
      setCertificates(ensureArray(unwrapApiData(response)));
    } catch (err) {
      setCertificates([]);
    } finally {
      setLoading(false);
    }
  };

  // Form handlers
  const handleCreateNews = async (e) => {
    e.preventDefault();
    try {
      await newsAPI.createNews(newsForm);
      setShowNewsModal(false);
      setNewsForm({ title: '', content: '', category: 'GENERAL' });
      setSuccess('News posted successfully!');
      notifyDataUpdated();
      loadNews();
    } catch (err) {
      setError('Failed to create news');
    }
  };

  const handleEditNews = (newsItem) => {
    setEditingNews(newsItem);
    setEditNewsForm({
      title: newsItem.title,
      content: newsItem.content,
      category: newsItem.category || 'GENERAL'
    });
    setShowEditNewsModal(true);
  };

  const handleUpdateNews = async (e) => {
    e.preventDefault();
    try {
      await newsAPI.updateNews(editingNews.id, editNewsForm);
      setShowEditNewsModal(false);
      setEditingNews(null);
      setEditNewsForm({ title: '', content: '', category: 'GENERAL' });
      setSuccess('News updated successfully!');
      notifyDataUpdated();
      loadNews();
    } catch (err) {
      setError('Failed to update news');
    }
  };

  const handleDeleteNews = async (id) => {
    if (!window.confirm('Are you sure you want to delete this news?')) return;
    try {
      await newsAPI.deleteNews(id);
      setSuccess('News deleted successfully!');
      notifyDataUpdated();
      loadNews();
    } catch (err) {
      setError('Failed to delete news');
    }
  };


  const handleCreateCenter = async (e) => {
    e.preventDefault();
    try {
      await adminAPI.createCenter(centerForm);
      setShowCenterModal(false);
      setCenterForm({ name: '', address: '', city: '', state: '', pincode: '', phone: '', email: '', workingHours: '', dailyCapacity: 100 });
      setSuccess('Center created successfully!');
      notifyDataUpdated();
      loadDashboardData();
      loadCenters();
    } catch (err) {
      setError('Failed to create center');
    }
  };

  const openEditCenterModal = (center) => {
    setEditingCenter(center);
    setEditCenterForm({
      name: center.name || '',
      address: center.address || '',
      city: center.city || '',
      state: center.state || '',
      pincode: center.pincode || '',
      phone: center.phone || '',
      email: center.email || '',
      workingHours: center.workingHours || '',
      dailyCapacity: center.dailyCapacity || 100
    });
    setShowEditCenterModal(true);
  };

  const handleUpdateCenter = async (e) => {
    e.preventDefault();
    try {
      await adminAPI.updateCenter(editingCenter.id, editCenterForm);
      setShowEditCenterModal(false);
      setEditingCenter(null);
      setSuccess('Center updated successfully!');
      notifyDataUpdated();
      loadDashboardData();
      loadCenters();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update center');
    }
  };

  // Ensure centers are loaded when opening drive modal
  const handleOpenDriveModal = async () => {
    if (centers.length === 0) {
      await loadCenters();
    }
    setShowDriveModal(true);
  };

  const handleCreateDrive = async (e) => {
    e.preventDefault();
    try {
      await adminAPI.createDrive({ ...driveForm, centerId: Number(driveForm.centerId) });
      setShowDriveModal(false);
      setDriveForm({ title: '', description: '', vaccineType: '', centerId: '', driveDate: '', minAge: 18, maxAge: 100, totalSlots: 100, status: 'UPCOMING' });
      setSuccess('Drive created successfully!');
      notifyDataUpdated();
      loadDashboardData();
      loadDrives();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create drive');
    }
  };

  const openEditDriveModal = async (drive) => {
    if (centers.length === 0) {
      await loadCenters();
    }
    setEditingDrive(drive);
    setEditDriveForm({
      title: drive.title || '',
      description: drive.description || '',
      vaccineType: drive.vaccineType || '',
      centerId: drive.center?.id || drive.centerId || '',
      driveDate: drive.driveDate || '',
      minAge: drive.minAge || 0,
      maxAge: drive.maxAge || 100,
      totalSlots: drive.totalSlots || 100,
      status: drive.status || 'UPCOMING'
    });
    setShowEditDriveModal(true);
  };

  const handleUpdateDrive = async (e) => {
    e.preventDefault();
    try {
      await adminAPI.updateDrive(editingDrive.id, { ...editDriveForm, centerId: Number(editDriveForm.centerId) });
      setShowEditDriveModal(false);
      setEditingDrive(null);
      setSuccess('Drive updated successfully!');
      notifyDataUpdated();
      loadDashboardData();
      loadDrives();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update drive');
    }
  };

  const handleDriveStatusChange = async (driveId, status) => {
    try {
      await adminAPI.updateDrive(driveId, { status });
      setDrives((current) => current.map((drive) => (drive.id === driveId ? { ...drive, status } : drive)));
      setSuccess(`Drive status updated to ${status}.`);
      notifyDataUpdated();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update drive status');
    }
  };

  // Open slot modal and ensure drives are loaded
  const handleOpenSlotModal = async (drive) => {
    if (drives.length === 0) {
      await loadDrives();
    }
    setSelectedDrive(drive || null);
    setSlotForm({ driveId: drive?.id || '', startTime: '', endTime: '', capacity: 50 });
    setShowSlotModal(true);
  };

  const handleCreateSlot = async (e) => {
    e.preventDefault();
    setSlotActionLoading(true);
    try {
      const slotData = buildSlotPayload(slotForm);
      await adminAPI.createSlot(slotData);
      setShowSlotModal(false);
      setSlotForm({ driveId: '', startTime: '', endTime: '', capacity: 50 });
      setSuccess('Slot created successfully!');
      notifyDataUpdated();
      loadDashboardData();
      loadDrives();
      loadSlots();
      if (selectedDrive?.id) {
        loadDriveSlots(selectedDrive.id, true);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create slot');
    } finally {
      setSlotActionLoading(false);
    }
  };

  const loadDriveSlots = async (driveId, keepOpen = false) => {
    try {
      setError(null);
      const response = await (isSuperAdmin ? superAdminAPI.getDriveSlots(driveId) : adminAPI.getDriveSlots(driveId));
      const payload = unwrapApiData(response) || {};
      const slots = (Array.isArray(payload) ? payload : (payload.slots || [])).map(normalizeSlotForEditing);
      setDriveSlots(slots);
      if (keepOpen) {
        setShowManageSlotsModal(true);
      }
    } catch (err) {
      console.error('Failed to load drive slots:', err.response?.data || err.message);
      setDriveSlots([]);
      setError('Failed to load slots');
    }
  };

  const openManageSlotsModal = async (drive) => {
    setSelectedDrive(drive);
    await loadDriveSlots(drive.id, true);
  };

  const openEditSlotModal = (slot) => {
    const normalizedSlot = normalizeSlotForEditing(slot);
    setEditingSlot(normalizedSlot);
    const isExpired = normalizedSlot.slotStatus === 'EXPIRED';
    setEditSlotForm({
      driveId: normalizedSlot.driveId || '',
      startTime: normalizedSlot.editStartTime || '',
      endTime: normalizedSlot.editEndTime || '',
      capacity: normalizedSlot.capacity || 50
    });
    if (isExpired) {
      setSuccess('Warning: you are editing an expired slot.');
    }
    setShowEditSlotModal(true);
  };

  const handleUpdateSlot = async (e) => {
    e.preventDefault();
    setSlotActionLoading(true);
    try {
      const slotPayload = buildSlotPayload(editSlotForm);
      const nextStatus = getSlotStatusPreview(slotPayload.startDate, slotPayload.endDate);
      if (!editingSlot?.id) {
        throw new Error('Slot ID is missing');
      }
      const response = await (isSuperAdmin ? superAdminAPI.updateSlot(editingSlot.id, slotPayload) : adminAPI.updateSlot(editingSlot.id, slotPayload));
      const updatedSlot = normalizeSlotForEditing(unwrapApiData(response));

      setSlots((currentSlots) => mergeUpdatedSlot(currentSlots, updatedSlot));
      setDriveSlots((currentSlots) => mergeUpdatedSlot(currentSlots, updatedSlot));

      setShowEditSlotModal(false);
      setEditingSlot(null);
      setEditSlotForm({ driveId: '', startTime: '', endTime: '', capacity: 50 });

      const updatedDriveId = Number(slotPayload.driveId);
      const updatedDrive = drives.find((drive) => Number(drive.id) === updatedDriveId) || selectedDrive;
      setSelectedDrive(updatedDrive || null);

      await Promise.all([
        loadDashboardData(),
        loadDrives(),
        loadSlots(),
        updatedDriveId ? loadDriveSlots(updatedDriveId, true) : Promise.resolve()
      ]);

      setSuccess(
        nextStatus === 'EXPIRED'
          ? 'Slot updated successfully, but the selected date/time is still in the past so it remains EXPIRED.'
          : `Slot updated successfully. New status: ${nextStatus || 'UPDATED'}.`
      );
      notifyDataUpdated();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update slot');
    } finally {
      setSlotActionLoading(false);
    }
  };

  const handleDeleteSlot = async (slotId) => {
    if (!window.confirm('Are you sure you want to delete this slot?')) return;
    try {
      await (isSuperAdmin ? superAdminAPI.deleteSlot(slotId) : adminAPI.deleteSlot(slotId));
      setSuccess('Slot deleted successfully!');
      notifyDataUpdated();
      loadDashboardData();
      loadDrives();
      loadSlots();
      if (selectedDrive?.id) {
        loadDriveSlots(selectedDrive.id, true);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete slot');
    }
  };

  const handleGenerateCertificate = async (e) => {
    e.preventDefault();
    try {
      await certificateAPI.generateCertificate(certificateForm);
      setShowCertificateModal(false);
      setCertificateForm({ bookingId: '', vaccineName: '', doseNumber: 1 });
      setSuccess('Certificate generated successfully!');
      loadCertificates();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to generate certificate');
    }
  };

  const handleToggleUser = async (userId, enabled) => {
    try {
      if (enabled) {
        await adminAPI.disableUser(userId);
      } else {
        await adminAPI.enableUser(userId);
      }
      setSuccess(`User ${enabled ? 'disabled' : 'enabled'} successfully!`);
      loadUsers();
    } catch (err) {
      setError('Failed to update user status');
    }
  };

  const handleUpdateBookingStatus = async (bookingId, status) => {
    try {
      let response;
      if (String(status).toLowerCase() === 'completed' || String(status).toLowerCase() === 'complete') {
        response = await adminAPI.completeBooking(bookingId);
      } else {
        response = await adminAPI.updateBookingStatus(bookingId, status);
      }
      const updatedBooking = unwrapApiData(response);
      if (updatedBooking?.id) {
        setBookings((current) => current.map((booking) => (
          booking.id === updatedBooking.id ? { ...booking, ...updatedBooking } : booking
        )));
        if (selectedBooking?.id === updatedBooking.id) {
          setSelectedBooking((current) => current ? { ...current, ...updatedBooking } : current);
        }
      }
      setSuccess(`Booking ${status} successfully!`);
      setShowEditBookingModal(false);
      await Promise.all([loadDashboardData(), loadBookings(), loadCertificates()]);
    } catch (err) {
      setError('Failed to update booking status');
    }
  };

  const handleDeleteBooking = async (bookingId) => {
    if (!window.confirm('Delete booking?')) return;

    try {
      await adminAPI.deleteBooking(bookingId);
      setBookings((current) => current.filter((booking) => booking.id !== bookingId));
      if (selectedBooking?.id === bookingId) {
        setSelectedBooking(null);
        setShowEditBookingModal(false);
      }
      setSuccess('Booking deleted successfully!');
      await Promise.all([loadDashboardData(), loadBookings(), loadCertificates()]);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete booking');
    }
  };

  const handleDeleteCenter = async (centerId) => {
    if (!window.confirm('Are you sure you want to delete this center?')) return;
    try {
      await (isSuperAdmin ? superAdminAPI.deleteCenter(centerId) : adminAPI.deleteCenter(centerId));
      setSuccess('Center deleted successfully!');
      notifyDataUpdated();
      loadDashboardData();
      loadCenters();
    } catch (err) {
      setError('Failed to delete center');
    }
  };

  const handleDeleteDrive = async (driveId) => {
    if (!window.confirm('Are you sure you want to delete this drive?')) return;
    try {
      await (isSuperAdmin ? superAdminAPI.deleteDrive(driveId) : adminAPI.deleteDrive(driveId));
      setSuccess('Drive deleted successfully!');
      notifyDataUpdated();
      loadDashboardData();
      loadDrives();
    } catch (err) {
      setError('Failed to delete drive');
    }
  };

  const openEditUserModal = (user) => {
    setEditingUser(user);
    setEditUserForm({
      email: user.email || '',
      fullName: user.fullName || '',
      age: user.age || 0,
      phoneNumber: user.phoneNumber || '',
      enabled: user.enabled !== false
    });
    setShowEditUserModal(true);
  };

  const handleUpdateUser = async (e) => {
    e.preventDefault();
    try {
      await superAdminAPI.updateUser(editingUser.id, editUserForm);
      setShowEditUserModal(false);
      setEditingUser(null);
      setSuccess('User updated successfully!');
      loadDashboardData();
      loadUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update user');
    }
  };

  const handleDeleteUser = async (userId) => {
    if (!window.confirm('Are you sure you want to delete this user?')) return;
    try {
      await superAdminAPI.deleteUser(userId);
      setSuccess('User deleted successfully!');
      loadDashboardData();
      loadUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete user');
    }
  };

  const openEditBooking = (booking) => {
    setSelectedBooking(booking);
    setShowEditBookingModal(true);
  };

  // Chart data
  const chartData = stats ? {
    labels: ['Users', 'Active Users', 'Bookings', 'Active Drives', 'Centers'],
    datasets: [{
      label: 'System Statistics',
      data: [stats.totalUsers || 0, stats.activeUsers || 0, stats.totalBookings || 0, stats.activeDrives || 0, stats.totalCenters || 0],
      backgroundColor: ['#0ea5e9', '#38bdf8', '#10b981', '#ef4444', '#f59e0b'],
      borderRadius: 8
    }]
  } : null;

  const bookingsByStatus = {
    labels: ['Pending', 'Confirmed', 'Completed', 'Cancelled'],
    datasets: [{
      data: [
        bookings.filter(b => b.status === 'PENDING').length,
        bookings.filter(b => b.status === 'CONFIRMED').length,
        bookings.filter(b => b.status === 'COMPLETED').length,
        bookings.filter(b => b.status === 'CANCELLED').length
      ],
      backgroundColor: ['#f59e0b', '#0ea5e9', '#10b981', '#ef4444']
    }]
  };

  const getStatusBadge = (status) => {
    const colors = {
      PENDING: 'warning',
      CONFIRMED: 'info',
      COMPLETED: 'success',
      CANCELLED: 'danger'
    };
    return <Badge bg={colors[status] || 'secondary'}>{status}</Badge>;
  };

  const filteredUsers = users.filter(user => 
    user.fullName?.toLowerCase().includes(debouncedSearchTerm.toLowerCase()) ||
    user.email?.toLowerCase().includes(debouncedSearchTerm.toLowerCase())
  );

  const filteredBookings = bookings.filter(booking => 
    booking.userName?.toLowerCase().includes(debouncedSearchTerm.toLowerCase()) ||
    booking.status?.toLowerCase().includes(debouncedSearchTerm.toLowerCase())
  );

  const getSlotStatusBadge = (status) => {
    const styles = {
      LIVE: { bg: 'success', text: 'LIVE' },
      UPCOMING: { bg: 'primary', text: 'UPCOMING' },
      EXPIRED: { bg: 'danger', text: 'EXPIRED' }
    };
    const config = styles[status] || { bg: 'secondary', text: status || 'UNKNOWN' };
    return <Badge bg={config.bg}>{config.text}</Badge>;
  };

  // Tab configuration
  const tabs = [
    { id: 'dashboard', label: 'Dashboard', icon: <FaChartLine /> },
    { id: 'users', label: 'Users', icon: <FaUsers /> },
    { id: 'bookings', label: 'Bookings', icon: <FaCalendarCheck /> },
    { id: 'slots', label: 'Manage Slots', icon: <FaCalendarCheck /> },
    { id: 'centers', label: 'Centers', icon: <FaHospital /> },
    { id: 'drives', label: 'Drives', icon: <FaSyringe /> },
    { id: 'certificates', label: 'Certificates', icon: <FaCertificate /> },
    { id: 'news', label: 'News', icon: <FaNewspaper /> },
    { id: 'feedback', label: 'Feedback', icon: <FaComment /> },
    { id: 'contacts', label: 'Contacts', icon: <FaPhone /> }
  ];

  // Load functions for Feedback and Contacts
  const loadFeedbacks = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getAllFeedback(0, 10);
      setFeedbacks(ensureArray(unwrapApiData(response)));
    } catch (err) {
      setError('Failed to load feedback');
      setFeedbacks([]);
    } finally {
      setLoading(false);
    }
  };

  const loadContacts = async () => {
    try {
      setLoading(true);
      const response = await adminAPI.getAllContacts();
      setContacts(ensureArray(unwrapApiData(response)));
    } catch (err) {
      setError('Failed to load contacts');
      setContacts([]);
    } finally {
      setLoading(false);
    }
  };

  const handleRespondToFeedback = async (id) => {
    if (!responseText.trim()) {
      setError('Please enter a response');
      return;
    }
    try {
      await adminAPI.respondToFeedback(id, responseText);
      setSuccess('Response sent successfully!');
      setShowFeedbackModal(false);
      setResponseText('');
      setSelectedFeedback(null);
      await loadFeedbacks();
      notifyDataUpdated();
    } catch (err) {
      setError('Failed to send response');
    }
  };

  const handleRespondToContact = async (id) => {
    if (!responseText.trim()) {
      setError('Please enter a response');
      return;
    }
    try {
      await adminAPI.respondToContact(id, responseText);
      setSuccess('Response sent successfully!');
      setShowContactModal(false);
      setResponseText('');
      setSelectedContact(null);
      await loadContacts();
      notifyDataUpdated();
    } catch (err) {
      setError('Failed to send response');
    }
  };

  const handleDeleteContact = async (id) => {
    if (!window.confirm('Are you sure you want to delete this contact?')) return;
    try {
      await adminAPI.deleteContact(id);
      setSuccess('Contact deleted successfully!');
      loadContacts();
    } catch (err) {
      setError('Failed to delete contact');
    }
  };

  const openFeedbackModal = (feedback) => {
    setSelectedFeedback(feedback);
    setResponseText(feedback.replyMessage || feedback.response || '');
    setShowFeedbackModal(true);
  };

  const openContactModal = (contact) => {
    setSelectedContact(contact);
    setResponseText(contact.replyMessage || contact.response || '');
    setShowContactModal(true);
  };

  const renderFeedback = () => (
    <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
      <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}} className="py-3">
        <h5 className="mb-0 fw-bold" style={{color: '#0ea5e9'}}><FaComment className="me-2" />User Feedback</h5>
      </Card.Header>
      <Card.Body className="p-0">
        {feedbacks.length === 0 ? (
          <div className="text-center py-5">
            <FaComment size={48} className="text-muted mb-3" />
            <p className="text-muted">No feedback yet.</p>
          </div>
        ) : (
          <Table responsive hover className="mb-0">
            <thead style={{background: '#f8fafc'}}>
              <tr>
                <th className="ps-4">ID</th>
                <th>User</th>
                <th>Type</th>
                <th>Subject</th>
                <th>Rating</th>
                <th>Status</th>
                <th className="text-end pe-4">Actions</th>
              </tr>
            </thead>
            <tbody>
              {feedbacks.map(feedback => (
                <tr key={feedback.id}>
                  <td className="ps-4">#{feedback.id}</td>
                  <td>
                    <div className="fw-semibold">{feedback.userName || 'Anonymous'}</div>
                    <div className="small text-muted">{feedback.userEmail || 'No email'}</div>
                  </td>
                  <td><Badge bg="info">{feedback.type || 'FEEDBACK'}</Badge></td>
                  <td>{feedback.subject || 'N/A'}</td>
                  <td>
                    {[...Array(5)].map((_, i) => (
                      <span key={i} style={{color: i < (feedback.rating || 0) ? '#f59e0b' : '#e2e8f0'}}>★</span>
                    ))}
                  </td>
                  <td>{communicationBadge(feedback.status)}</td>
                  <td className="text-end pe-4">
                    <Button variant="outline-primary" size="sm" onClick={() => openFeedbackModal(feedback)} style={{borderRadius: '0.375rem'}}>
                      <FaEdit /> {feedback.replyMessage || feedback.response ? 'Edit Reply' : 'Reply'}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card.Body>
    </Card>
  );

  const renderContacts = () => (
    <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
      <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}} className="py-3">
        <h5 className="mb-0 fw-bold" style={{color: '#0ea5e9'}}><FaPhone className="me-2" />Contact Inquiries</h5>
      </Card.Header>
      <Card.Body className="p-0">
        {contacts.length === 0 ? (
          <div className="text-center py-5">
            <FaPhone size={48} className="text-muted mb-3" />
            <p className="text-muted">No contact inquiries yet.</p>
          </div>
        ) : (
          <Table responsive hover className="mb-0">
            <thead style={{background: '#f8fafc'}}>
              <tr>
                <th className="ps-4">ID</th>
                <th>User</th>
                <th>Type</th>
                <th>Phone</th>
                <th>Message</th>
                <th>Date</th>
                <th>Status</th>
                <th className="text-end pe-4">Actions</th>
              </tr>
            </thead>
            <tbody>
              {contacts.map(contact => (
                <tr key={contact.id}>
                  <td className="ps-4">#{contact.id}</td>
                  <td>
                    <div className="fw-semibold">{contact.userName || contact.name || 'Guest'}</div>
                    <div className="small text-muted">{contact.userEmail || contact.email || 'No email'}</div>
                  </td>
                  <td><Badge bg="secondary">{contact.type || 'CONTACT'}</Badge></td>
                  <td>{contact.phone || 'N/A'}</td>
                  <td>{contact.message?.substring(0, 30)}{contact.message && contact.message.length > 30 ? '...' : ''}</td>
                <td>{formatDate(contact.createdAt)}</td>
                  <td>{communicationBadge(contact.status)}</td>
                  <td className="text-end pe-4">
                    <Button variant="outline-primary" size="sm" className="me-2" onClick={() => openContactModal(contact)} style={{borderRadius: '0.375rem'}}>
                      <FaEdit /> {contact.replyMessage || contact.response ? 'Edit Reply' : 'Reply'}
                    </Button>
                    <Button variant="outline-danger" size="sm" onClick={() => handleDeleteContact(contact.id)} style={{borderRadius: '0.375rem'}}>
                      <FaTrash />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card.Body>
    </Card>
  );

  const renderDashboard = () => (
    <>
      {/* Stats Cards - Matching user dashboard style */}
      <Row className="mb-4 g-4">
        <Col md={6} lg={3}>
          <div className="stats-card" style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)'}}>
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <div className="stat-label" style={{opacity: 0.9}}>Total Users</div>
                <div className="stat-number">{stats?.totalUsers || 0}</div>
              </div>
              <FaUsers size={40} style={{opacity: 0.3}} />
            </div>
          </div>
        </Col>
        <Col md={6} lg={3}>
          <div className="stats-card" style={{background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)'}}>
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <div className="stat-label" style={{opacity: 0.9}}>Total Bookings</div>
                <div className="stat-number">{stats?.totalBookings || 0}</div>
              </div>
              <FaCalendarCheck size={40} style={{opacity: 0.3}} />
            </div>
          </div>
        </Col>
        <Col md={6} lg={3}>
          <div className="stats-card" style={{background: 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)'}}>
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <div className="stat-label" style={{opacity: 0.9}}>Active Drives</div>
                <div className="stat-number">{stats?.activeDrives || 0}</div>
              </div>
              <FaSyringe size={40} style={{opacity: 0.3}} />
            </div>
          </div>
        </Col>
        <Col md={6} lg={3}>
          <div className="stats-card" style={{background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)'}}>
            <div className="d-flex align-items-center justify-content-between">
              <div>
                <div className="stat-label" style={{opacity: 0.9}}>Centers</div>
                <div className="stat-number">{stats?.totalCenters || 0}</div>
              </div>
              <FaHospital size={40} style={{opacity: 0.3}} />
            </div>
          </div>
        </Col>
      </Row>

      {/* Charts */}
      <Row className="mb-4 g-4">
        <Col md={8}>
          <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
            <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}}>
              <h5 className="mb-0 fw-bold" style={{color: '#0ea5e9'}}><FaChartLine className="me-2" />System Overview</h5>
            </Card.Header>
            <Card.Body>
              <Bar 
                data={chartData} 
                options={{ 
                  responsive: true, 
                  maintainAspectRatio: false,
                  plugins: {
                    legend: { display: false }
                  },
                  scales: {
                    y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' } },
                    x: { grid: { display: false } }
                  }
                }} 
                height={300}
              />
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card className="border-0 shadow-sm h-100" style={{borderRadius: '0.75rem'}}>
            <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}}>
              <h5 className="mb-0 fw-bold" style={{color: '#0ea5e9'}}><FaCalendarCheck className="me-2" />Bookings by Status</h5>
            </Card.Header>
            <Card.Body className="d-flex justify-content-center align-items-center">
              <Doughnut 
                data={bookingsByStatus} 
                options={{ 
                  responsive: true, 
                  maintainAspectRatio: false,
                  plugins: {
                    legend: { position: 'bottom' }
                  }
                }} 
                height={250}
              />
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Quick Stats */}
      <Row className="g-4">
        <Col md={4}>
          <Card className="border-0 shadow-sm h-100" style={{borderRadius: '0.75rem'}}>
            <Card.Body className="text-center py-4">
              <div style={{width: '60px', height: '60px', margin: '0 auto 1rem', background: 'linear-gradient(135deg, #e0f2fe 0%, #0ea5e9 100%)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                <FaUsers size={24} style={{color: '#0ea5e9'}} />
              </div>
              <h2 className="fw-bold mb-1" style={{color: '#0ea5e9'}}>{stats?.newUsersThisMonth || 0}</h2>
              <p className="text-muted mb-0">New Users This Month</p>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card className="border-0 shadow-sm h-100" style={{borderRadius: '0.75rem'}}>
            <Card.Body className="text-center py-4">
              <div style={{width: '60px', height: '60px', margin: '0 auto 1rem', background: 'linear-gradient(135deg, #d1fae5 0%, #10b981 100%)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                <FaCalendarCheck size={24} style={{color: '#10b981'}} />
              </div>
              <h2 className="fw-bold mb-1" style={{color: '#10b981'}}>{stats?.bookingsToday || 0}</h2>
              <p className="text-muted mb-0">Bookings Today</p>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card className="border-0 shadow-sm h-100" style={{borderRadius: '0.75rem'}}>
            <Card.Body className="text-center py-4">
              <div style={{width: '60px', height: '60px', margin: '0 auto 1rem', background: 'linear-gradient(135deg, #cffafe 0%, #06b6d4 100%)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                <FaCertificate size={24} style={{color: '#06b6d4'}} />
              </div>
              <h2 className="fw-bold mb-1" style={{color: '#06b6d4'}}>{stats?.completedVaccinations || 0}</h2>
              <p className="text-muted mb-0">Vaccinations Completed</p>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </>
  );

  const renderUsers = () => (
    <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
      <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}} className="py-3">
        <Row className="align-items-center">
          <Col md={6}>
            <h5 className="mb-0 fw-bold" style={{color: '#0ea5e9'}}><FaUsers className="me-2" />User Management</h5>
          </Col>
          <Col md={6}>
            <div className="d-flex gap-2">
              <Form.Control 
                type="text" 
                placeholder="Search users..." 
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{borderRadius: '0.5rem'}}
              />
            </div>
          </Col>
        </Row>
      </Card.Header>
      <Card.Body className="p-0">
        <Table responsive hover className="mb-0">
          <thead style={{background: '#f8fafc'}}>
            <tr>
              <th className="ps-4">ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Age</th>
              <th>Status</th>
              <th>Verified</th>
              <th className="text-end pe-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.slice(currentPage * 10, (currentPage + 1) * 10).map(user => (
              <tr key={user.id}>
                <td className="ps-4">#{user.id}</td>
                <td className="fw-medium">{user.fullName}</td>
                <td>{user.email}</td>
                <td>{user.age}</td>
                <td><Badge bg={user.enabled ? 'success' : 'danger'} style={user.enabled ? {background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)'} : {background: 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)'}}>{user.enabled ? 'Active' : 'Disabled'}</Badge></td>
                <td>
                  {user.emailVerified ? 
                    <Badge bg="success" style={{background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)'}}><FaCheck className="me-1" />Yes</Badge> : 
                    <Badge bg="warning" style={{background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)'}}><FaTimes className="me-1" />No</Badge>
                  }
                </td>
                <td className="text-end pe-4">
                  <div className="d-flex justify-content-end gap-2">
                    {isSuperAdmin && (
                      <>
                        <Button variant="outline-primary" size="sm" onClick={() => openEditUserModal(user)} style={{borderRadius: '0.375rem'}}>
                          <FaEdit />
                        </Button>
                        <Button variant="outline-danger" size="sm" onClick={() => handleDeleteUser(user.id)} style={{borderRadius: '0.375rem'}}>
                          <FaTrash />
                        </Button>
                      </>
                    )}
                    <Button 
                      variant={user.enabled ? 'outline-danger' : 'outline-success'} 
                      size="sm"
                      onClick={() => handleToggleUser(user.id, user.enabled)}
                      style={{borderRadius: '0.375rem'}}
                    >
                      {user.enabled ? 'Disable' : 'Enable'}
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      </Card.Body>
    </Card>
  );

  const renderBookings = () => (
    <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
      <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}} className="py-3">
        <Row className="align-items-center">
          <Col md={6}>
            <h5 className="mb-0 fw-bold" style={{color: '#0ea5e9'}}><FaCalendarCheck className="me-2" />Booking Management</h5>
          </Col>
          <Col md={6}>
            <Form.Control 
              type="text" 
              placeholder="Search bookings..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{borderRadius: '0.5rem'}}
            />
          </Col>
        </Row>
      </Card.Header>
      <Card.Body className="p-0">
        <Table responsive hover className="mb-0">
          <thead style={{background: '#f8fafc'}}>
            <tr>
              <th className="ps-4">ID</th>
              <th>User</th>
              <th>Slot</th>
              <th>Status</th>
              <th>Booked At</th>
              <th className="text-end pe-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredBookings.map(booking => (
              <tr key={booking.id}>
                <td className="ps-4">#{booking.id}</td>
                <td className="fw-medium">{booking.userName || 'N/A'}</td>
                <td>{booking.slotTime || 'N/A'}</td>
                <td>{getStatusBadge(booking.status)}</td>
                  <td>{formatDate(booking.bookedAt)}</td>
                <td className="text-end pe-4">
                  <Button variant="outline-primary" size="sm" className="me-2" onClick={() => openEditBooking(booking)} style={{borderRadius: '0.375rem'}}>
                    <FaEdit />
                  </Button>
                  {booking.status === 'PENDING' && (
                    <>
                      <Button variant="outline-success" size="sm" className="me-1" onClick={() => handleUpdateBookingStatus(booking.id, 'confirmed')} style={{borderRadius: '0.375rem'}}>
                        <FaCheck />
                      </Button>
                      <Button variant="outline-danger" size="sm" onClick={() => handleUpdateBookingStatus(booking.id, 'cancelled')} style={{borderRadius: '0.375rem'}}>
                        <FaTimes />
                      </Button>
                    </>
                  )}
                  {booking.status === 'CONFIRMED' && (
                    <>
                      <Button variant="outline-success" size="sm" className="me-1" onClick={() => handleUpdateBookingStatus(booking.id, 'completed')} style={{borderRadius: '0.375rem'}}>
                        <FaCertificate />
                      </Button>
                      <Button variant="outline-danger" size="sm" onClick={() => handleUpdateBookingStatus(booking.id, 'cancelled')} style={{borderRadius: '0.375rem'}}>
                        <FaTimes />
                      </Button>
                    </>
                  )}
                  <Button variant="outline-danger" size="sm" onClick={() => handleDeleteBooking(booking.id)} style={{borderRadius: '0.375rem'}}>
                    <FaTrash />
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      </Card.Body>
    </Card>
  );

  const renderCenters = () => (
    <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
      <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}} className="py-3 d-flex justify-content-between align-items-center">
        <h5 className="mb-0 fw-bold" style={{color: '#0ea5e9'}}><FaHospital className="me-2" />Vaccination Centers</h5>
        <Button style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}} onClick={() => setShowCenterModal(true)}>
          <FaPlus className="me-2" />Add Center
        </Button>
      </Card.Header>
      <Card.Body>
        <Row className="g-4">
          {centers.map(center => (
            <Col key={center.id} md={6} lg={4}>
              <Card className="h-100 border-0 shadow-sm" style={{borderRadius: '0.75rem', transition: 'transform 0.2s, box-shadow 0.2s'}}>
                <Card.Body>
                  <div className="d-flex justify-content-between align-items-start mb-3">
                    <Card.Title className="h6 mb-0 fw-bold">{center.name}</Card.Title>
                    <Badge style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)'}}>{center.dailyCapacity || 0} doses/day</Badge>
                  </div>
                  <div className="text-muted small">
                    <div className="d-flex align-items-center gap-2 mb-2">
                      <FaHospital style={{color: '#0ea5e9'}} />
                      <span>{center.address}, {center.city}</span>
                    </div>
                    <div className="d-flex align-items-center gap-2 mb-2">
                      <FaBell style={{color: '#0ea5e9'}} />
                      <span>Phone: {center.phone || 'N/A'}</span>
                    </div>
                    {center.workingHours && (
                      <div className="d-flex align-items-center gap-2">
                        <FaCog style={{color: '#0ea5e9'}} />
                        <span>Hours: {center.workingHours}</span>
                      </div>
                    )}
                  </div>
                  <div className="d-flex gap-2 mt-3">
                    <Button variant="outline-primary" size="sm" onClick={() => openEditCenterModal(center)} style={{borderRadius: '0.375rem'}}>
                      <FaEdit className="me-1" />Edit
                    </Button>
                    <Button variant="outline-danger" size="sm" onClick={() => handleDeleteCenter(center.id)} style={{borderRadius: '0.375rem'}}>
                      <FaTrash className="me-1" />Delete
                    </Button>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      </Card.Body>
    </Card>
  );

  const renderDrives = () => (
    <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
      <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}} className="py-3 d-flex justify-content-between align-items-center">
        <h5 className="mb-0 fw-bold" style={{color: '#0ea5e9'}}><FaSyringe className="me-2" />Vaccination Drives</h5>
        <Button style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}} onClick={handleOpenDriveModal}>
          <FaPlus className="me-2" />Add Drive
        </Button>
      </Card.Header>
      <Card.Body className="p-0">
        <Table responsive hover className="mb-0">
          <thead style={{background: '#f8fafc'}}>
            <tr>
              <th className="ps-4">ID</th>
              <th>Title</th>
              <th>Center</th>
              <th>Date</th>
              <th>Age Range</th>
              <th>Status</th>
              <th className="text-end pe-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {drives.map(drive => (
              <tr key={drive.id}>
                <td className="ps-4">#{drive.id}</td>
                <td className="fw-medium">{drive.title}</td>
                <td>{drive.center?.name || drive.centerName || 'N/A'}</td>
                <td>{drive.driveDate}</td>
                <td>{drive.minAge} - {drive.maxAge}</td>
                <td>
                  <Form.Select
                    size="sm"
                    value={drive.status || 'UPCOMING'}
                    onChange={(e) => handleDriveStatusChange(drive.id, e.target.value)}
                    style={{borderRadius: '0.5rem', minWidth: '140px'}}
                  >
                    {DRIVE_STATUS_OPTIONS.map(status => <option key={status} value={status}>{status}</option>)}
                  </Form.Select>
                </td>
                <td className="text-end pe-4">
                  <div className="d-flex justify-content-end gap-2">
                    <Button variant="outline-primary" size="sm" onClick={() => openEditDriveModal(drive)} style={{borderRadius: '0.375rem'}}>
                      <FaEdit />
                    </Button>
                    <Button variant="outline-info" size="sm" onClick={() => openManageSlotsModal(drive)} style={{borderRadius: '0.375rem'}}>
                      Slots
                    </Button>
                    <Button variant="outline-primary" size="sm" onClick={() => handleOpenSlotModal(drive)} style={{borderRadius: '0.375rem'}}>
                      <FaPlus />
                    </Button>
                    <Button variant="outline-danger" size="sm" onClick={() => handleDeleteDrive(drive.id)} style={{borderRadius: '0.375rem'}}>
                      <FaTrash />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      </Card.Body>
    </Card>
  );

  const renderSlots = () => (
    <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
      <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}} className="py-3 d-flex justify-content-between align-items-center">
        <div>
          <h5 className="mb-1 fw-bold" style={{color: '#0ea5e9'}}><FaCalendarCheck className="me-2" />Manage Slots</h5>
          <small className="text-muted">Monitor live, upcoming, and expired slots in one place.</small>
        </div>
        <Button style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}} onClick={() => handleOpenSlotModal(null)}>
          <FaPlus className="me-2" />Add Slot
        </Button>
      </Card.Header>
      <Card.Body>
        <Row className="g-3 mb-4">
          <Col md={3}>
            <Form.Group>
              <Form.Label>Status</Form.Label>
              <Form.Select value={slotFilters.status} onChange={(e) => setSlotFilters((current) => ({ ...current, status: e.target.value }))} style={{borderRadius: '0.5rem'}}>
                <option value="ALL">All</option>
                <option value="LIVE">Live</option>
                <option value="UPCOMING">Upcoming</option>
                <option value="EXPIRED">Expired</option>
              </Form.Select>
            </Form.Group>
          </Col>
          <Col md={3}>
            <Form.Group>
              <Form.Label>Center</Form.Label>
              <Form.Select value={slotFilters.centerId} onChange={(e) => setSlotFilters((current) => ({ ...current, centerId: e.target.value, driveId: '' }))} style={{borderRadius: '0.5rem'}}>
                <option value="">All Centers</option>
                {centers.map((center) => (
                  <option key={center.id} value={center.id}>{center.name}</option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
          <Col md={3}>
            <Form.Group>
              <Form.Label>Drive</Form.Label>
              <Form.Select value={slotFilters.driveId} onChange={(e) => setSlotFilters((current) => ({ ...current, driveId: e.target.value }))} style={{borderRadius: '0.5rem'}}>
                <option value="">All Drives</option>
                {drives
                  .filter((drive) => !slotFilters.centerId || String(drive.centerId || drive.center?.id || '') === String(slotFilters.centerId))
                  .map((drive) => (
                    <option key={drive.id} value={drive.id}>{drive.title}</option>
                  ))}
              </Form.Select>
            </Form.Group>
          </Col>
          <Col md={3}>
            <Form.Group>
              <Form.Label>Date</Form.Label>
              <Form.Control type="date" value={slotFilters.date} onChange={(e) => setSlotFilters((current) => ({ ...current, date: e.target.value }))} style={{borderRadius: '0.5rem'}} />
            </Form.Group>
          </Col>
        </Row>

        <div className="d-flex justify-content-between align-items-center mb-3">
          <small className="text-muted">{slots.length} slot{slots.length === 1 ? '' : 's'} found</small>
          <Button variant="outline-secondary" size="sm" onClick={() => setSlotFilters({ status: 'ALL', centerId: '', driveId: '', date: '' })} style={{borderRadius: '0.5rem'}}>
            Reset Filters
          </Button>
        </div>

        {slots.length === 0 ? (
          <div className="text-center py-5">
            <FaCalendarCheck size={48} className="text-muted mb-3" />
            <p className="text-muted mb-0">No slots matched the current filters.</p>
          </div>
        ) : (
          <Table responsive hover className="mb-0">
            <thead style={{background: '#f8fafc'}}>
              <tr>
                <th className="ps-4">Slot Time</th>
                <th>Center</th>
                <th>Drive</th>
                <th>Capacity</th>
                <th>Status</th>
                <th className="text-end pe-4">Actions</th>
              </tr>
            </thead>
            <tbody>
              {slots.map((slot) => (
                <tr key={slot.id}>
                  <td className="ps-4">
                    <div className="fw-medium">{formatDateTime(slot.startDate || slot.time || slot.dateTime)}</div>
                    <small className="text-muted">Ends {formatDateTime(slot.endDate || slot.endTime)}</small>
                  </td>
                  <td>{slot.centerName || 'N/A'}</td>
                  <td>{slot.driveName || 'N/A'}</td>
                  <td>{slot.bookedCount || 0} / {slot.capacity || 0}</td>
                  <td>{getSlotStatusBadge(slot.slotStatus)}</td>
                  <td className="text-end pe-4">
                    <div className="d-flex justify-content-end gap-2">
                      <Button variant="outline-primary" size="sm" onClick={() => openEditSlotModal(slot)} style={{borderRadius: '0.375rem'}}>
                        <FaEdit />
                      </Button>
                      <Button variant="outline-danger" size="sm" onClick={() => handleDeleteSlot(slot.id)} style={{borderRadius: '0.375rem'}}>
                        <FaTrash />
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card.Body>
    </Card>
  );

  const renderCertificates = () => (
    <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
      <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}} className="py-3 d-flex justify-content-between align-items-center">
        <h5 className="mb-0 fw-bold" style={{color: '#0ea5e9'}}><FaCertificate className="me-2" />Certificate Management</h5>
        <Button style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}} onClick={() => setShowCertificateModal(true)}>
          <FaPlus className="me-2" />Generate Certificate
        </Button>
      </Card.Header>
      <Card.Body className="p-0">
        {certificates.length === 0 ? (
          <div className="text-center py-5">
            <FaCertificate size={48} className="text-muted mb-3" />
            <p className="text-muted">No certificates generated yet.</p>
            <Button style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none'}} onClick={() => setShowCertificateModal(true)}>
              Generate First Certificate
            </Button>
          </div>
        ) : (
          <Table responsive hover className="mb-0">
            <thead style={{background: '#f8fafc'}}>
              <tr>
                <th className="ps-4">Certificate #</th>
                <th>User</th>
                <th>Vaccine</th>
                <th>Dose</th>
                <th>Center</th>
                <th>Issued Date</th>
                <th className="text-end pe-4">Actions</th>
              </tr>
            </thead>
            <tbody>
              {certificates.map(cert => (
                <tr key={cert.id}>
                  <td className="ps-4 fw-medium">{cert.certificateNumber}</td>
                  <td>{cert.userName}</td>
                  <td>{cert.vaccineName}</td>
                  <td>Dose {cert.doseNumber}</td>
                  <td>{cert.centerName}</td>
                  <td>{formatDate(cert.issuedAt)}</td>
                  <td className="text-end pe-4">
                    <Button variant="outline-primary" size="sm" onClick={() => window.open(`/certificates/${cert.id}`, '_blank')} style={{borderRadius: '0.375rem'}}>
                      <FaDownload />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card.Body>
    </Card>
  );

  const renderNews = () => (
    <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
      <Card.Header style={{background: 'linear-gradient(135deg, #e0f2fe 0%, #f0f9ff 100%)', borderBottom: '1px solid rgba(14, 165, 233, 0.1)'}} className="py-3 d-flex justify-content-between align-items-center">
        <h5 className="mb-0 fw-bold" style={{color: '#0ea5e9'}}><FaNewspaper className="me-2" />News Management</h5>
        <Button style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}} onClick={() => setShowNewsModal(true)}>
          <FaPlus className="me-2" />Post News
        </Button>
      </Card.Header>
      <Card.Body className="p-0">
        <div className="table-responsive">
          <Table hover className="mb-0">
            <thead style={{background: '#f8fafc'}}>
              <tr>
                <th>Title</th>
                <th>Category</th>
                <th>Date</th>
                <th>Status</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>
            <tbody>
              {news.length === 0 ? (
                <tr>
                  <td colSpan="5" className="text-center py-5">
                    <FaNewspaper size={48} className="text-muted mb-3 d-block" />
                    <p className="text-muted mb-4">No news posts yet.</p>
                    <Button style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none'}} onClick={() => setShowNewsModal(true)}>
                      Post First News
                    </Button>
                  </td>
                </tr>
              ) : news.map(item => (
                <tr key={item.id}>
                  <td className="fw-medium">{item.title}</td>
                  <td><Badge style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)'}}>{item.category || 'GENERAL'}</Badge></td>
                  <td>{item.publishedAt ? new Date(item.publishedAt).toLocaleDateString() : 'N/A'}</td>
                  <td>
                    <Badge bg={item.active ? 'success' : 'secondary'}>
                      {item.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </td>
                  <td className="text-end">
                    <Button 
                      variant="outline-primary" 
                      size="sm" 
                      className="me-2" 
                      onClick={() => handleEditNews(item)}
                      style={{borderRadius: '0.375rem'}}
                    >
                      Edit
                    </Button>
                    <Button 
                      variant="outline-danger" 
                      size="sm"
                      onClick={() => handleDeleteNews(item.id)}
                      style={{borderRadius: '0.375rem'}}
                    >
                      Delete
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>
      </Card.Body>

    </Card>
  );

  if (loading && !stats.totalUsers && !stats.totalBookings && !stats.activeDrives && !stats.totalCenters) {
    return (
      <div className="d-flex align-items-center justify-content-center" style={{minHeight: '60vh'}}>
        <div className="text-center">
          <Spinner animation="border" style={{color: '#0ea5e9'}} />
          <p className="mt-3 text-muted">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-pattern" style={{minHeight: '100vh', paddingBottom: '2rem'}}>
      {/* Admin Header - Matching user dashboard hero style */}
      <div style={{
        background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)',
        padding: '3rem 0',
        marginBottom: '2rem',
        position: 'relative',
        overflow: 'hidden'
      }}>
        <div style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'url("data:image/svg+xml,%3Csvg width=\'60\' height=\'60\' viewBox=\'0 0 60 60\' xmlns=\'http://www.w3.org/2000/svg\'%3E%3Cg fill=\'none\' fill-rule=\'evenodd\'%3E%3Cg fill=\'%23ffffff\' fill-opacity=\'0.05\'%3E%3Cpath d=\'M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z\'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")'
        }}></div>
        <Container position="relative">
          <Row className="align-items-center">
            <Col md={8}>
              <div className="d-flex align-items-center gap-3 mb-3">
                <div style={{
                  width: '70px',
                  height: '70px',
                  background: 'rgba(255,255,255,0.2)',
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  <FaUserShield size={32} style={{color: 'white'}} />
                </div>
                <div>
                  <h1 className="mb-1 fw-bold text-white" style={{fontSize: '2rem'}}>Admin Dashboard</h1>
                  <p className="mb-0 text-white-50">Manage your vaccination system</p>
                </div>
              </div>
            </Col>
            <Col md={4} className="text-md-end">
              <Button 
                variant="light" 
                onClick={loadDashboardData}
                style={{borderRadius: '0.5rem'}}
              >
                <FaCog className="me-2" />Refresh Data
              </Button>
            </Col>
          </Row>
        </Container>
      </div>

      <Container>
        {/* Alerts */}
        {error && <Alert variant="danger" dismissible onClose={() => setError(null)} className="mb-4" style={{borderRadius: '0.5rem', border: 'none'}}>{error}</Alert>}
        {success && <Alert variant="success" dismissible onClose={() => setSuccess(null)} className="mb-4" style={{borderRadius: '0.5rem', border: 'none', background: 'linear-gradient(135deg, #d1fae5 0%, #10b981 100%)', color: '#065f46'}}>{success}</Alert>}

        {/* Tab Navigation - Matching user dashboard style */}
        <div className="mb-4" style={{
          background: 'white',
          borderRadius: '0.75rem',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          padding: '0.5rem'
        }}>
          <Tabs activeKey={activeTab} onSelect={setActiveTab} className="justify-content-center">
            {tabs.map(tab => (
              <Tab 
                key={tab.id} 
                eventKey={tab.id} 
                title={
                  <span className={`d-flex align-items-center gap-2 ${activeTab === tab.id ? '' : 'text-muted'}`}>
                    {tab.icon}
                    {tab.label}
                  </span>
                } 
              />
            ))}
          </Tabs>
        </div>

        {/* Tab Content */}
        {activeTab === 'dashboard' && (
          error && !loading && !stats.totalUsers && !stats.totalBookings && !stats.activeDrives && !stats.totalCenters ? (
            <Card className="border-0 shadow-sm" style={{borderRadius: '0.75rem'}}>
              <Card.Body className="py-5">
                <ErrorState
                  title="Dashboard data is unavailable"
                  message={error}
                  onRetry={loadDashboardData}
                />
              </Card.Body>
            </Card>
          ) : renderDashboard()
        )}
        {activeTab === 'users' && renderUsers()}
        {activeTab === 'bookings' && renderBookings()}
        {activeTab === 'slots' && renderSlots()}
        {activeTab === 'centers' && renderCenters()}
        {activeTab === 'drives' && renderDrives()}
        {activeTab === 'certificates' && renderCertificates()}
        {activeTab === 'news' && renderNews()}
        {activeTab === 'feedback' && renderFeedback()}
        {activeTab === 'contacts' && renderContacts()}
      </Container>

      {/* Create News Modal */}
      <Modal show={showNewsModal} onHide={() => setShowNewsModal(false)} size="lg" centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaNewspaper className="me-2" />Post News</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateNews}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Title</Form.Label>
              <Form.Control type="text" value={newsForm.title} onChange={e => setNewsForm({...newsForm, title: e.target.value})} required placeholder="Enter news title" style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Category</Form.Label>
              <Form.Select value={newsForm.category} onChange={e => setNewsForm({...newsForm, category: e.target.value})} style={{borderRadius: '0.5rem'}}>
                <option value="GENERAL">General</option>
                <option value="HEALTH">Health</option>
                <option value="VACCINATION">Vaccination</option>
                <option value="UPDATE">Update</option>
              </Form.Select>
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Content</Form.Label>
              <Form.Control as="textarea" rows={4} value={newsForm.content} onChange={e => setNewsForm({...newsForm, content: e.target.value})} required placeholder="Write your news content..." style={{borderRadius: '0.5rem'}} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowNewsModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
            <Button type="submit" style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>Post News</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Edit News Modal */}
      <Modal show={showEditNewsModal} onHide={() => setShowEditNewsModal(false)} size="lg" centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaNewspaper className="me-2" />Edit News</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleUpdateNews}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Title</Form.Label>
              <Form.Control type="text" value={editNewsForm.title} onChange={e => setEditNewsForm({...editNewsForm, title: e.target.value})} required style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Category</Form.Label>
              <Form.Select value={editNewsForm.category} onChange={e => setEditNewsForm({...editNewsForm, category: e.target.value})} style={{borderRadius: '0.5rem'}}>
                <option value="GENERAL">General</option>
                <option value="HEALTH">Health</option>
                <option value="VACCINATION">Vaccination</option>
                <option value="UPDATE">Update</option>
              </Form.Select>
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Content</Form.Label>
              <Form.Control as="textarea" rows={4} value={editNewsForm.content} onChange={e => setEditNewsForm({...editNewsForm, content: e.target.value})} required style={{borderRadius: '0.5rem'}} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowEditNewsModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
            <Button type="submit" style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>Update News</Button>
          </Modal.Footer>
        </Form>
      </Modal>


      {/* Center Modal */}
      <Modal show={showCenterModal} onHide={() => setShowCenterModal(false)} size="lg" centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaHospital className="me-2" />Add Vaccination Center</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateCenter}>
          <Modal.Body>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Center Name</Form.Label>
                  <Form.Control type="text" value={centerForm.name} onChange={e => setCenterForm({...centerForm, name: e.target.value})} required placeholder="Enter center name" style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Phone</Form.Label>
                  <Form.Control type="text" value={centerForm.phone} onChange={e => setCenterForm({...centerForm, phone: e.target.value})} required placeholder="Phone number" style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3">
              <Form.Label>Address</Form.Label>
              <Form.Control type="text" value={centerForm.address} onChange={e => setCenterForm({...centerForm, address: e.target.value})} required placeholder="Full address" style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Row>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>City</Form.Label>
                  <Form.Control type="text" value={centerForm.city} onChange={e => setCenterForm({...centerForm, city: e.target.value})} required style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>State</Form.Label>
                  <Form.Control type="text" value={centerForm.state} onChange={e => setCenterForm({...centerForm, state: e.target.value})} style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Pincode</Form.Label>
                  <Form.Control type="text" value={centerForm.pincode} onChange={e => setCenterForm({...centerForm, pincode: e.target.value})} style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Email</Form.Label>
                  <Form.Control type="email" value={centerForm.email} onChange={e => setCenterForm({...centerForm, email: e.target.value})} placeholder="center@example.com" style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Daily Capacity</Form.Label>
                  <Form.Control type="number" value={centerForm.dailyCapacity} onChange={e => setCenterForm({...centerForm, dailyCapacity: e.target.value})} style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3">
              <Form.Label>Working Hours</Form.Label>
              <Form.Control type="text" value={centerForm.workingHours} onChange={e => setCenterForm({...centerForm, workingHours: e.target.value})} placeholder="e.g., 09:00 AM - 05:00 PM" style={{borderRadius: '0.5rem'}} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowCenterModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
            <Button type="submit" style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>Create Center</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={showEditCenterModal} onHide={() => setShowEditCenterModal(false)} size="lg" centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaHospital className="me-2" />Edit Vaccination Center</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleUpdateCenter}>
          <Modal.Body>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Center Name</Form.Label>
                  <Form.Control type="text" value={editCenterForm.name} onChange={e => setEditCenterForm({...editCenterForm, name: e.target.value})} required style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Phone</Form.Label>
                  <Form.Control type="text" value={editCenterForm.phone} onChange={e => setEditCenterForm({...editCenterForm, phone: e.target.value})} required style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3">
              <Form.Label>Address</Form.Label>
              <Form.Control type="text" value={editCenterForm.address} onChange={e => setEditCenterForm({...editCenterForm, address: e.target.value})} required style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Row>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>City</Form.Label>
                  <Form.Control type="text" value={editCenterForm.city} onChange={e => setEditCenterForm({...editCenterForm, city: e.target.value})} required style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>State</Form.Label>
                  <Form.Control type="text" value={editCenterForm.state} onChange={e => setEditCenterForm({...editCenterForm, state: e.target.value})} style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Pincode</Form.Label>
                  <Form.Control type="text" value={editCenterForm.pincode} onChange={e => setEditCenterForm({...editCenterForm, pincode: e.target.value})} style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Email</Form.Label>
                  <Form.Control type="email" value={editCenterForm.email} onChange={e => setEditCenterForm({...editCenterForm, email: e.target.value})} style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Daily Capacity</Form.Label>
                  <Form.Control type="number" value={editCenterForm.dailyCapacity} onChange={e => setEditCenterForm({...editCenterForm, dailyCapacity: Number(e.target.value)})} style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3">
              <Form.Label>Working Hours</Form.Label>
              <Form.Control type="text" value={editCenterForm.workingHours} onChange={e => setEditCenterForm({...editCenterForm, workingHours: e.target.value})} style={{borderRadius: '0.5rem'}} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowEditCenterModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
            <Button type="submit" style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>Update Center</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Drive Modal */}
      <Modal show={showDriveModal} onHide={() => setShowDriveModal(false)} size="lg" centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaSyringe className="me-2" />Add Vaccination Drive</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateDrive}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Drive Title</Form.Label>
              <Form.Control type="text" value={driveForm.title} onChange={e => setDriveForm({...driveForm, title: e.target.value})} required placeholder="e.g., COVID-19 Booster Drive" style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control as="textarea" rows={2} value={driveForm.description} onChange={e => setDriveForm({...driveForm, description: e.target.value})} placeholder="Drive description" style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Vaccine Type</Form.Label>
              <Form.Control type="text" value={driveForm.vaccineType} onChange={e => setDriveForm({...driveForm, vaccineType: e.target.value})} placeholder="e.g., Covishield" style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Center</Form.Label>
                  <Form.Select value={driveForm.centerId} onChange={e => setDriveForm({...driveForm, centerId: e.target.value})} required style={{borderRadius: '0.5rem'}}>
                    <option value="">Select Center</option>
                    {centers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Date</Form.Label>
                  <Form.Control type="date" value={driveForm.driveDate} onChange={e => setDriveForm({...driveForm, driveDate: e.target.value})} required style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Min Age</Form.Label>
                  <Form.Control type="number" value={driveForm.minAge} onChange={e => setDriveForm({...driveForm, minAge: e.target.value})} min="0" style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Max Age</Form.Label>
                  <Form.Control type="number" value={driveForm.maxAge} onChange={e => setDriveForm({...driveForm, maxAge: e.target.value})} min="0" style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Status</Form.Label>
                  <Form.Select value={driveForm.status} onChange={e => setDriveForm({...driveForm, status: e.target.value})} style={{borderRadius: '0.5rem'}}>
                    {DRIVE_STATUS_OPTIONS.map(status => <option key={status} value={status}>{status}</option>)}
                  </Form.Select>
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3">
              <Form.Label>Total Slots</Form.Label>
              <Form.Control type="number" value={driveForm.totalSlots} onChange={e => setDriveForm({...driveForm, totalSlots: Number(e.target.value)})} min="1" style={{borderRadius: '0.5rem'}} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowDriveModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
            <Button type="submit" style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>Create Drive</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={showEditDriveModal} onHide={() => setShowEditDriveModal(false)} size="lg" centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaSyringe className="me-2" />Edit Vaccination Drive</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleUpdateDrive}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Drive Title</Form.Label>
              <Form.Control type="text" value={editDriveForm.title} onChange={e => setEditDriveForm({...editDriveForm, title: e.target.value})} required style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control as="textarea" rows={2} value={editDriveForm.description} onChange={e => setEditDriveForm({...editDriveForm, description: e.target.value})} style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Vaccine Type</Form.Label>
              <Form.Control type="text" value={editDriveForm.vaccineType} onChange={e => setEditDriveForm({...editDriveForm, vaccineType: e.target.value})} style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Center</Form.Label>
                  <Form.Select value={editDriveForm.centerId} onChange={e => setEditDriveForm({...editDriveForm, centerId: Number(e.target.value)})} required style={{borderRadius: '0.5rem'}}>
                    <option value="">Select Center</option>
                    {centers.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3">
                  <Form.Label>Date</Form.Label>
                  <Form.Control type="date" value={editDriveForm.driveDate} onChange={e => setEditDriveForm({...editDriveForm, driveDate: e.target.value})} required style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Min Age</Form.Label>
                  <Form.Control type="number" value={editDriveForm.minAge} onChange={e => setEditDriveForm({...editDriveForm, minAge: Number(e.target.value)})} min="0" style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Max Age</Form.Label>
                  <Form.Control type="number" value={editDriveForm.maxAge} onChange={e => setEditDriveForm({...editDriveForm, maxAge: Number(e.target.value)})} min="0" style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Total Slots</Form.Label>
                  <Form.Control type="number" value={editDriveForm.totalSlots} onChange={e => setEditDriveForm({...editDriveForm, totalSlots: Number(e.target.value)})} min="1" style={{borderRadius: '0.5rem'}} />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3">
              <Form.Label>Status</Form.Label>
              <Form.Select value={editDriveForm.status} onChange={e => setEditDriveForm({...editDriveForm, status: e.target.value})} style={{borderRadius: '0.5rem'}}>
                {DRIVE_STATUS_OPTIONS.map(status => <option key={status} value={status}>{status}</option>)}
              </Form.Select>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowEditDriveModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
            <Button type="submit" style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>Update Drive</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Slot Modal */}
      <Modal show={showSlotModal} onHide={() => setShowSlotModal(false)} size="lg" centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaCalendarCheck className="me-2" />Create Slot</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleCreateSlot}>
          <Modal.Body>
            <Alert variant="info" style={{background: 'linear-gradient(135deg, #cffafe 0%, #06b6d4 100%)', border: 'none', borderRadius: '0.5rem'}}>
              <small>{selectedDrive?.title ? <>Creating slot for drive: <strong>{selectedDrive.title}</strong></> : 'Choose a drive, date/time, and capacity to create a slot.'}</small>
            </Alert>
            <Form.Group className="mb-3">
              <Form.Label>Drive</Form.Label>
              <Form.Select value={slotForm.driveId} onChange={e => setSlotForm({...slotForm, driveId: e.target.value})} required style={{borderRadius: '0.5rem'}}>
                <option value="">Select Drive</option>
                {drives.map(drive => <option key={drive.id} value={drive.id}>{drive.title}</option>)}
              </Form.Select>
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Start Time</Form.Label>
              <Form.Control 
                type="datetime-local" 
                value={slotForm.startTime} 
                onChange={e => setSlotForm({...slotForm, startTime: e.target.value})} 
                required 
                style={{borderRadius: '0.5rem'}}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>End Time</Form.Label>
              <Form.Control 
                type="datetime-local" 
                value={slotForm.endTime} 
                onChange={e => setSlotForm({...slotForm, endTime: e.target.value})} 
                required 
                style={{borderRadius: '0.5rem'}}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Capacity (Number of Appointments)</Form.Label>
              <Form.Control 
                type="number" 
                value={slotForm.capacity} 
                onChange={e => setSlotForm({...slotForm, capacity: parseInt(e.target.value)})} 
                required 
                min="1"
                placeholder="e.g., 50"
                style={{borderRadius: '0.5rem'}}
              />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowSlotModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
            <Button type="submit" disabled={slotActionLoading} style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>{slotActionLoading ? 'Saving...' : 'Create Slot'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={showManageSlotsModal} onHide={() => setShowManageSlotsModal(false)} size="lg" centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaCalendarCheck className="me-2" />Manage Slots for {selectedDrive?.title}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {driveSlots.length === 0 ? (
            <p className="text-muted mb-0">No slots available for this drive yet.</p>
          ) : (
            <Table responsive hover className="mb-0">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Start</th>
                  <th>End</th>
                  <th>Capacity</th>
                  <th>Booked</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {driveSlots.map(slot => (
                  <tr key={slot.id}>
                    <td>#{slot.id}</td>
                    <td>{formatDateTime(slot.startDate || slot.dateTime || slot.time || slot.startTime)}</td>
                    <td>{formatSlotEndDisplay(slot)}</td>
                    <td>{slot.capacity}</td>
                    <td>{slot.bookedCount || 0}</td>
                    <td className="text-end">
                      <div className="d-flex justify-content-end gap-2">
                        <Button variant="outline-primary" size="sm" onClick={() => openEditSlotModal(slot)} style={{borderRadius: '0.375rem'}}>
                          <FaEdit />
                        </Button>
                        <Button variant="outline-danger" size="sm" onClick={() => handleDeleteSlot(slot.id)} style={{borderRadius: '0.375rem'}}>
                          <FaTrash />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowManageSlotsModal(false)} style={{borderRadius: '0.5rem'}}>Close</Button>
        </Modal.Footer>
      </Modal>

      <Modal show={showEditSlotModal} onHide={() => setShowEditSlotModal(false)} size="lg" centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaCalendarCheck className="me-2" />Edit Slot</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleUpdateSlot}>
          <Modal.Body>
            <Alert variant={getSlotStatusPreview(editSlotForm.startTime, editSlotForm.endTime) === 'EXPIRED' ? 'warning' : 'info'} style={{borderRadius: '0.5rem'}}>
              {(() => {
                const previewStatus = getSlotStatusPreview(editSlotForm.startTime, editSlotForm.endTime);
                if (previewStatus === 'EXPIRED') {
                  return 'This slot is editable, but the selected date/time is still before March 22, 2026, so it will remain EXPIRED after saving.';
                }
                if (previewStatus === 'LIVE') {
                  return 'Saving these values will make this slot LIVE immediately.';
                }
                if (previewStatus === 'UPCOMING') {
                  return 'Saving these values will make this slot UPCOMING.';
                }
                return 'Edit the slot date/time and capacity, then save your changes.';
              })()}
            </Alert>
            <Form.Group className="mb-3">
              <Form.Label>Drive</Form.Label>
              <Form.Select value={editSlotForm.driveId} onChange={e => setEditSlotForm({...editSlotForm, driveId: e.target.value})} required style={{borderRadius: '0.5rem'}}>
                <option value="">Select Drive</option>
                {drives.map(drive => <option key={drive.id} value={drive.id}>{drive.title}</option>)}
              </Form.Select>
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Start Time</Form.Label>
              <Form.Control type="datetime-local" value={editSlotForm.startTime} onChange={e => setEditSlotForm({...editSlotForm, startTime: e.target.value})} required style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>End Time</Form.Label>
              <Form.Control type="datetime-local" value={editSlotForm.endTime} onChange={e => setEditSlotForm({...editSlotForm, endTime: e.target.value})} required style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Capacity</Form.Label>
              <Form.Control type="number" value={editSlotForm.capacity} onChange={e => setEditSlotForm({...editSlotForm, capacity: Number(e.target.value)})} min="1" required style={{borderRadius: '0.5rem'}} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowEditSlotModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
            <Button type="submit" disabled={slotActionLoading} style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>{slotActionLoading ? 'Saving...' : 'Update Slot'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={showEditUserModal} onHide={() => setShowEditUserModal(false)} centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaUsers className="me-2" />Edit User</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleUpdateUser}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Full Name</Form.Label>
              <Form.Control type="text" value={editUserForm.fullName} onChange={e => setEditUserForm({...editUserForm, fullName: e.target.value})} required style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control type="email" value={editUserForm.email} onChange={e => setEditUserForm({...editUserForm, email: e.target.value})} required style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Age</Form.Label>
              <Form.Control type="number" min="0" value={editUserForm.age} onChange={e => setEditUserForm({...editUserForm, age: Number(e.target.value)})} required style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Phone Number</Form.Label>
              <Form.Control type="text" value={editUserForm.phoneNumber} onChange={e => setEditUserForm({...editUserForm, phoneNumber: e.target.value})} style={{borderRadius: '0.5rem'}} />
            </Form.Group>
            <Form.Group>
              <Form.Label>Enabled</Form.Label>
              <Form.Check type="switch" checked={editUserForm.enabled} onChange={e => setEditUserForm({...editUserForm, enabled: e.target.checked})} label={editUserForm.enabled ? 'Yes' : 'No'} />
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowEditUserModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
            <Button type="submit" style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>Update User</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Certificate Modal */}
      <Modal show={showCertificateModal} onHide={() => setShowCertificateModal(false)} size="lg" centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaCertificate className="me-2" />Generate Certificate</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleGenerateCertificate}>
          <Modal.Body>
            <Alert variant="info" style={{background: 'linear-gradient(135deg, #cffafe 0%, #06b6d4 100%)', border: 'none', borderRadius: '0.5rem'}}>
              <small>Generate a vaccination certificate for a completed booking. The booking must be in COMPLETED status.</small>
            </Alert>
            <Form.Group className="mb-3">
              <Form.Label>Booking ID</Form.Label>
              <Form.Control 
                type="number" 
                value={certificateForm.bookingId} 
                onChange={e => setCertificateForm({...certificateForm, bookingId: e.target.value})} 
                required 
                placeholder="Enter booking ID"
                style={{borderRadius: '0.5rem'}}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Vaccine Name</Form.Label>
              <Form.Control 
                type="text" 
                value={certificateForm.vaccineName} 
                onChange={e => setCertificateForm({...certificateForm, vaccineName: e.target.value})} 
                required 
                placeholder="e.g., Covishield, Covaxin, Pfizer"
                style={{borderRadius: '0.5rem'}}
              />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Dose Number</Form.Label>
              <Form.Select 
                value={certificateForm.doseNumber} 
                onChange={e => setCertificateForm({...certificateForm, doseNumber: parseInt(e.target.value)})}
                style={{borderRadius: '0.5rem'}}
              >
                <option value={1}>Dose 1</option>
                <option value={2}>Dose 2</option>
                <option value={3}>Booster Dose</option>
              </Form.Select>
            </Form.Group>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={() => setShowCertificateModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
            <Button type="submit" style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>Generate Certificate</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Edit Booking Modal */}
      <Modal show={showEditBookingModal} onHide={() => setShowEditBookingModal(false)} centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title>Update Booking Status</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>Booking ID: <strong>#{selectedBooking?.id}</strong></p>
          <p>Current Status: {getStatusBadge(selectedBooking?.status)}</p>
          <hr />
          <p className="mb-2">Change status to:</p>
          <div className="d-flex gap-2 flex-wrap">
            {selectedBooking?.status === 'PENDING' && (
              <>
                <Button variant="success" onClick={() => handleUpdateBookingStatus(selectedBooking.id, 'confirmed')} style={{borderRadius: '0.5rem'}}>
                  <FaCheck className="me-1" /> Confirm
                </Button>
                <Button variant="danger" onClick={() => handleUpdateBookingStatus(selectedBooking.id, 'cancelled')} style={{borderRadius: '0.5rem'}}>
                  <FaTimes className="me-1" /> Cancel
                </Button>
              </>
            )}
            {selectedBooking?.status === 'CONFIRMED' && (
              <>
                <Button variant="success" onClick={() => handleUpdateBookingStatus(selectedBooking.id, 'completed')} style={{borderRadius: '0.5rem'}}>
                  <FaCertificate className="me-1" /> Mark Completed
                </Button>
                <Button variant="secondary" onClick={() => handleUpdateBookingStatus(selectedBooking.id, 'cancelled')} style={{borderRadius: '0.5rem'}}>
                  Cancel Booking
                </Button>
              </>
            )}
            {selectedBooking?.id && (
              <Button variant="outline-danger" onClick={() => handleDeleteBooking(selectedBooking.id)} style={{borderRadius: '0.5rem'}}>
                <FaTrash className="me-1" /> Delete Booking
              </Button>
            )}
          </div>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowEditBookingModal(false)} style={{borderRadius: '0.5rem'}}>Close</Button>
        </Modal.Footer>
      </Modal>

      {/* Feedback Response Modal */}
      <Modal show={showFeedbackModal} onHide={() => setShowFeedbackModal(false)} centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaComment className="me-2" />Reply to Feedback</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p><strong>From:</strong> {selectedFeedback?.userName || 'Anonymous'} ({selectedFeedback?.userEmail || 'No email'})</p>
          <p><strong>Type:</strong> {selectedFeedback?.type || 'FEEDBACK'}</p>
          <p><strong>Subject:</strong> {selectedFeedback?.subject || 'N/A'}</p>
          <p><strong>Rating:</strong> {[...Array(5)].map((_, i) => <span key={i} style={{color: i < (selectedFeedback?.rating || 0) ? '#f59e0b' : '#e2e8f0'}}>★</span>)}</p>
          <hr />
          <p className="mb-2"><strong>Feedback:</strong></p>
          <div className="bg-light p-3 mb-3 rounded" style={{fontSize: '0.9rem'}}>
            {selectedFeedback?.message || 'No message'}
          </div>
          {(selectedFeedback?.replyMessage || selectedFeedback?.response) && (
            <div className="bg-success-subtle p-3 mb-3 rounded" style={{fontSize: '0.9rem'}}>
              <strong>Existing reply:</strong> {selectedFeedback?.replyMessage || selectedFeedback?.response}
            </div>
          )}
          <Form.Group>
            <Form.Label>Your Reply</Form.Label>
            <Form.Control 
              as="textarea" 
              rows={4} 
              value={responseText} 
              onChange={(e) => setResponseText(e.target.value)}
              placeholder="Write your reply to this feedback..."
              style={{borderRadius: '0.5rem'}}
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowFeedbackModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
          <Button onClick={() => handleRespondToFeedback(selectedFeedback?.id)} style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>Send Reply</Button>
        </Modal.Footer>
      </Modal>

      {/* Contact Response Modal */}
      <Modal show={showContactModal} onHide={() => setShowContactModal(false)} centered>
        <Modal.Header closeButton style={{background: '#f8fafc'}}>
          <Modal.Title><FaPhone className="me-2" />Reply to Contact</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p><strong>Name:</strong> {selectedContact?.userName || selectedContact?.name}</p>
          <p><strong>Email:</strong> {selectedContact?.userEmail || selectedContact?.email}</p>
          <p><strong>Type:</strong> {selectedContact?.type || 'CONTACT'}</p>
          <p><strong>Phone:</strong> {selectedContact?.phone || 'N/A'}</p>
          <p><strong>Subject:</strong> {selectedContact?.subject || 'N/A'}</p>
          <hr />
          <p className="mb-2"><strong>Message:</strong></p>
          <div className="bg-light p-3 mb-3 rounded" style={{fontSize: '0.9rem'}}>
            {selectedContact?.message || 'No message'}
          </div>
          {(selectedContact?.replyMessage || selectedContact?.response) && (
            <div className="bg-success-subtle p-3 mb-3 rounded" style={{fontSize: '0.9rem'}}>
              <strong>Existing reply:</strong> {selectedContact?.replyMessage || selectedContact?.response}
            </div>
          )}
          <Form.Group>
            <Form.Label>Your Reply</Form.Label>
            <Form.Control 
              as="textarea" 
              rows={4} 
              value={responseText} 
              onChange={(e) => setResponseText(e.target.value)}
              placeholder="Write your reply to this inquiry..."
              style={{borderRadius: '0.5rem'}}
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowContactModal(false)} style={{borderRadius: '0.5rem'}}>Cancel</Button>
          <Button onClick={() => handleRespondToContact(selectedContact?.id)} style={{background: 'linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%)', border: 'none', borderRadius: '0.5rem'}}>Send Reply</Button>
        </Modal.Footer>
      </Modal>

      {/* Custom Styles */}
      <style>{`
        .nav-tabs .nav-link {
          border: none;
          padding: 0.875rem 1.5rem;
          color: #64748b;
          font-weight: 600;
          border-radius: 0.5rem;
          transition: all 0.2s;
        }
        .nav-tabs .nav-link:hover {
          background: #e0f2fe;
          color: #0ea5e9;
        }
        .nav-tabs .nav-link.active {
          background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
          color: white;
          border-radius: 0.5rem;
        }
        .stats-card {
          border-radius: 0.75rem;
          padding: 1.5rem;
          color: white;
          transition: transform 0.2s, box-shadow 0.2s;
        }
        .stats-card:hover {
          transform: translateY(-4px);
          box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.2);
        }
        .stats-card .stat-number {
          font-size: 2.5rem;
          font-weight: 800;
          line-height: 1;
        }
        .stats-card .stat-label {
          font-size: 0.875rem;
          text-transform: uppercase;
          letter-spacing: 0.5px;
          font-weight: 500;
        }
        .table thead th {
          background: #f8fafc;
          border-bottom: 2px solid #0ea5e9;
          color: #1e293b;
          font-weight: 600;
          text-transform: uppercase;
          font-size: 0.75rem;
          letter-spacing: 0.5px;
          padding: 1rem;
        }
        .table tbody td {
          padding: 1rem;
          vertical-align: middle;
          border-color: #e2e8f0;
        }
        .table tbody tr:hover {
          background: #e0f2fe;
        }
        .card {
          transition: transform 0.2s, box-shadow 0.2s;
        }
        .card:hover {
          transform: translateY(-2px);
        }
      `}</style>
    </div>
  );
}
