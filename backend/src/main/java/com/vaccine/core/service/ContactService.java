package com.vaccine.core.service;

import com.vaccine.common.dto.AnalyticsPointResponse;
import com.vaccine.common.dto.ApiMessage;
import com.vaccine.common.dto.ContactAnalyticsResponse;
import com.vaccine.common.dto.ContactRequest;
import com.vaccine.common.dto.ContactSummaryResponse;
import com.vaccine.domain.Contact;
import com.vaccine.domain.ContactStatus;
import com.vaccine.domain.User;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.BookingRepository;
import com.vaccine.infrastructure.persistence.repository.ContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ContactService {
    private final ContactRepository contactRepository;
    private final BookingRepository bookingRepository;
    private final CommunicationNotificationService communicationNotificationService;

    public ContactService(ContactRepository contactRepository,
                          BookingRepository bookingRepository,
                          CommunicationNotificationService communicationNotificationService) {
        this.contactRepository = contactRepository;
        this.bookingRepository = bookingRepository;
        this.communicationNotificationService = communicationNotificationService;
    }

    @Transactional
    public ApiMessage submitContact(ContactRequest request, User user) {
        if (request == null) {
            throw new IllegalArgumentException("Contact request is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Message is required");
        }

        Contact contact = new Contact();
        contact.setName(request.name().trim());
        contact.setEmail(request.email().trim().toLowerCase());
        contact.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        contact.setSubject(request.subject() == null || request.subject().isBlank() ? "General inquiry" : request.subject().trim());
        contact.setMessage(request.message().trim());
        contact.setUser(user);
        contact.setStatus(ContactStatus.PENDING);
        contact.setAdminId(resolveContactOwnerAdminId(user));

        contactRepository.save(contact);

        return new ApiMessage("Contact inquiry submitted successfully. We will respond soon.");
    }

    @Transactional
    public List<Map<String, Object>> getUserInquiries(User user) {
        if (user == null || user.getId() == null) {
            throw new AppException("User not found");
        }

        Map<Long, Contact> contactsById = new LinkedHashMap<>();
        List<Contact> contacts = new ArrayList<>(contactRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()));

        String userEmail = user.getEmail() == null ? null : user.getEmail().trim().toLowerCase();
        if (userEmail != null && !userEmail.isBlank()) {
            List<Contact> orphanContacts = contactRepository.findByUserIsNullAndEmailIgnoreCaseOrderByCreatedAtDesc(userEmail);
            if (!orphanContacts.isEmpty()) {
                orphanContacts.forEach(contact -> contact.setUser(user));
                contactRepository.saveAll(orphanContacts);
                contacts.addAll(orphanContacts);
            }
        }

        contacts.stream()
            .sorted(Comparator.comparing(Contact::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Contact::getId, Comparator.nullsLast(Comparator.reverseOrder())))
            .forEach(contact -> contactsById.putIfAbsent(contact.getId(), contact));

        return contactsById.values().stream().map(this::toMap).toList();
    }

    public List<Map<String, Object>> getAllContacts() {
        return getAllContacts(null);
    }

    public List<Map<String, Object>> getAllContacts(Long adminId) {
        List<Contact> contacts = adminId == null
            ? contactRepository.findAllByOrderByCreatedAtDescIdDesc()
            : contactRepository.findByAdminIdOrderByCreatedAtDescIdDesc(adminId);
        if (contacts.isEmpty() && adminId == null) {
            contacts = contactRepository.findAll();
        }
        return contacts.stream().map(this::toMap).toList();
    }

    public ContactAnalyticsResponse getContactAnalytics() {
        return getContactAnalytics(null);
    }

    public ContactAnalyticsResponse getContactAnalytics(Long adminId) {
        List<Contact> contacts = adminId == null
            ? contactRepository.findAllByOrderByCreatedAtDescIdDesc()
            : contactRepository.findByAdminIdOrderByCreatedAtDescIdDesc(adminId);
        if (contacts.isEmpty() && adminId == null) {
            contacts = contactRepository.findAll();
        }
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        long totalInquiries = contacts.size();
        long todayInquiries = contacts.stream()
            .filter(contact -> contact.getCreatedAt() != null && contact.getCreatedAt().toLocalDate().isEqual(today))
            .count();
        long weeklyInquiries = contacts.stream()
            .filter(contact -> contact.getCreatedAt() != null && !contact.getCreatedAt().toLocalDate().isBefore(weekStart))
            .count();

        Map<LocalDate, Long> dailyCounts = new TreeMap<>();
        for (int offset = 6; offset >= 0; offset--) {
            dailyCounts.put(today.minusDays(offset), 0L);
        }
        contacts.stream()
            .filter(contact -> contact.getCreatedAt() != null)
            .map(contact -> contact.getCreatedAt().toLocalDate())
            .filter(date -> !date.isBefore(weekStart))
            .forEach(date -> dailyCounts.computeIfPresent(date, (key, count) -> count + 1));

        List<AnalyticsPointResponse> inquiriesByDay = dailyCounts.entrySet().stream()
            .map(entry -> new AnalyticsPointResponse(entry.getKey().toString(), entry.getValue()))
            .toList();

        List<AnalyticsPointResponse> mostActiveUsers = contacts.stream()
            .collect(Collectors.groupingBy(this::resolveContactActor, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
            .limit(5)
            .map(entry -> new AnalyticsPointResponse(entry.getKey(), entry.getValue()))
            .toList();

        List<ContactSummaryResponse> recentInquiries = contacts.stream()
            .sorted(Comparator.comparing(Contact::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(8)
            .map(contact -> new ContactSummaryResponse(
                contact.getId(),
                contact.getUser() != null && contact.getUser().getFullName() != null ? contact.getUser().getFullName() : contact.getName(),
                contact.getUser() != null && contact.getUser().getEmail() != null ? contact.getUser().getEmail() : contact.getEmail(),
                contact.getSubject(),
                contact.getMessage(),
                contact.getStatus().name(),
                contact.getCreatedAt()
            ))
            .toList();

        return new ContactAnalyticsResponse(totalInquiries, todayInquiries, weeklyInquiries, inquiriesByDay, mostActiveUsers, recentInquiries);
    }

    public Map<String, Object> getContactById(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new AppException("Contact not found"));
        return toMap(contact);
    }

    private Long resolveContactOwnerAdminId(User user) {
        if (user == null) {
            return null;
        }
        if (bookingRepository == null) {
            return null;
        }
        if (user.isAdmin() && !user.isSuperAdmin()) {
            return user.getId();
        }
        return bookingRepository.findByUserId(user.getId()).stream()
            .map(ContactService::resolveBookingAdminId)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private static Long resolveBookingAdminId(com.vaccine.domain.Booking booking) {
        if (booking.getAdminId() != null) {
            return booking.getAdminId();
        }
        if (booking.getSlot() != null && booking.getSlot().getAdminId() != null) {
            return booking.getSlot().getAdminId();
        }
        if (booking.getSlot() != null && booking.getSlot().getDrive() != null) {
            return booking.getSlot().getDrive().getAdminId();
        }
        return null;
    }

    @Transactional
    public ApiMessage respondToContact(Long id, String response) {
        if (response == null || response.isBlank()) {
            throw new AppException("Response is required");
        }

        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new AppException("Contact not found"));

        contact.setResponse(response.trim());
        contact.setStatus(ContactStatus.REPLIED);
        contactRepository.save(contact);
        if (contact.getUser() != null) {
            communicationNotificationService.notifyReply(
                contact.getUser(),
                "CONTACT",
                "Reply to your contact message",
                contact.getMessage(),
                contact.getResponse(),
                contact.getId()
            );
        }

        return new ApiMessage("Response sent successfully");
    }

    @Transactional
    public ApiMessage deleteContact(Long id) {
        contactRepository.deleteById(id);
        return new ApiMessage("Contact deleted successfully");
    }

    private Map<String, Object> toMap(Contact contact) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", contact.getId());
        map.put("userId", contact.getUser() != null ? contact.getUser().getId() : null);
        map.put("adminId", contact.getAdminId());
        map.put("name", contact.getName());
        map.put("email", contact.getEmail());
        map.put("userName", contact.getUser() != null && contact.getUser().getFullName() != null ? contact.getUser().getFullName() : contact.getName());
        map.put("userEmail", contact.getUser() != null && contact.getUser().getEmail() != null ? contact.getUser().getEmail() : contact.getEmail());
        map.put("phone", contact.getPhone());
        map.put("subject", contact.getSubject());
        map.put("message", contact.getMessage());
        map.put("response", contact.getResponse());
        map.put("adminReply", contact.getResponse());
        map.put("replyMessage", contact.getResponse());
        map.put("status", contact.getStatus().name());
        map.put("type", "CONTACT");
        map.put("createdAt", contact.getCreatedAt());
        map.put("updatedAt", contact.getUpdatedAt());
        return map;
    }

    private String resolveContactActor(Contact contact) {
        if (contact.getUser() != null && contact.getUser().getFullName() != null && !contact.getUser().getFullName().isBlank()) {
            return contact.getUser().getFullName().trim();
        }
        if (contact.getName() != null && !contact.getName().isBlank()) {
            return contact.getName().trim();
        }
        if (contact.getEmail() != null && !contact.getEmail().isBlank()) {
            return contact.getEmail().trim().toLowerCase();
        }
        return "Anonymous";
    }
}
