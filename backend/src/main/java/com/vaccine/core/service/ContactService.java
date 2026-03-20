package com.vaccine.core.service;

import com.vaccine.common.dto.ApiMessage;
import com.vaccine.common.dto.ContactRequest;
import com.vaccine.domain.Contact;
import com.vaccine.domain.ContactStatus;
import com.vaccine.domain.User;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.ContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContactService {
    private final ContactRepository contactRepository;
    private final CommunicationNotificationService communicationNotificationService;

    public ContactService(ContactRepository contactRepository, CommunicationNotificationService communicationNotificationService) {
        this.contactRepository = contactRepository;
        this.communicationNotificationService = communicationNotificationService;
    }

    @Transactional
    public ApiMessage submitContact(ContactRequest request, User user) {
        // Validate required fields
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
        contact.setName(request.name());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setSubject(request.subject());
        contact.setMessage(request.message());
        contact.setUser(user);
        contact.setStatus(ContactStatus.PENDING);

        contactRepository.save(contact);

        return new ApiMessage("Contact inquiry submitted successfully. We will respond soon.");
    }

    public List<Map<String, Object>> getUserInquiries(Long userId) {
        List<Contact> contacts = contactRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return contacts.stream().map(this::toMap).toList();
    }

    public List<Map<String, Object>> getAllContacts() {
        return contactRepository.findAll().stream().map(this::toMap).toList();
    }

    public Map<String, Object> getContactById(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new AppException("Contact not found"));
        return toMap(contact);
    }

    @Transactional
    public ApiMessage respondToContact(Long id, String response) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new AppException("Contact not found"));

        contact.setResponse(response);
        contact.setStatus(ContactStatus.REPLIED);
        contactRepository.save(contact);
        communicationNotificationService.notifyReply(
            contact.getUser(),
            "CONTACT",
            "Reply to your contact message",
            contact.getMessage(),
            response,
            contact.getId()
        );

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
        map.put("name", contact.getName());
        map.put("email", contact.getEmail());
        map.put("userName", contact.getUser() != null && contact.getUser().getFullName() != null ? contact.getUser().getFullName() : contact.getName());
        map.put("userEmail", contact.getUser() != null && contact.getUser().getEmail() != null ? contact.getUser().getEmail() : contact.getEmail());
        map.put("phone", contact.getPhone());
        map.put("subject", contact.getSubject());
        map.put("message", contact.getMessage());
        map.put("response", contact.getResponse());
        map.put("replyMessage", contact.getResponse());
        map.put("status", contact.getStatus().name());
        map.put("type", "CONTACT");
        map.put("createdAt", contact.getCreatedAt());
        map.put("updatedAt", contact.getUpdatedAt());
        return map;
    }
}
