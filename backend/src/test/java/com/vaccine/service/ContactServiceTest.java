package com.vaccine.service;

import com.vaccine.common.dto.ApiMessage;
import com.vaccine.common.dto.ContactRequest;
import com.vaccine.core.service.CommunicationNotificationService;
import com.vaccine.domain.Contact;
import com.vaccine.domain.User;
import com.vaccine.infrastructure.persistence.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.vaccine.core.service.ContactService;
import com.vaccine.domain.ContactStatus;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;
    @Mock
    private CommunicationNotificationService communicationNotificationService;

    @InjectMocks
    private ContactService contactService;

    private User testUser;
    private Contact testContact;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        testContact = new Contact();
        testContact.setId(1L);
        testContact.setUser(testUser);
        testContact.setName("Test User");
        testContact.setEmail("test@example.com");
        testContact.setPhone("+91-1234567890");
        testContact.setSubject("General Inquiry");
        testContact.setMessage("I have a question about vaccination");
        testContact.setStatus(ContactStatus.PENDING);
        testContact.setCreatedAt(LocalDateTime.now());
    }

    // ==================== POSITIVE TESTS ====================

    @Test
    void submitContact_Success() {
        ContactRequest request = new ContactRequest(
            "Test User",
            "test@example.com",
            "+91-1234567890",
            "General Inquiry",
            "I have a question about vaccination"
        );

        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        ApiMessage result = contactService.submitContact(request, testUser);

        assertNotNull(result);
        assertEquals("Contact inquiry submitted successfully. We will respond soon.", result.message());
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void submitContact_WithoutPhone_Success() {
        ContactRequest request = new ContactRequest(
            "Test User",
            "test@example.com",
            null,
            "General Inquiry",
            "I have a question about vaccination"
        );

        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        ApiMessage result = contactService.submitContact(request, testUser);

        assertNotNull(result);
    }

    @Test
    void submitContact_WithoutUser_Success() {
        ContactRequest request = new ContactRequest(
            "Guest User",
            "guest@example.com",
            null,
            "General Inquiry",
            "I have a question"
        );

        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        ApiMessage result = contactService.submitContact(request, null);

        assertNotNull(result);
    }

    @Test
    void getUserInquiries_Success() {
        when(contactRepository.findByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testContact));
        when(contactRepository.findByUserIsNullAndEmailIgnoreCaseOrderByCreatedAtDesc("test@example.com")).thenReturn(List.of());

        List<Map<String, Object>> result = contactService.getUserInquiries(testUser);

        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).get("email"));
    }

    @Test
    void getAllContacts_Success() {
        when(contactRepository.findAll()).thenReturn(List.of(testContact));

        List<Map<String, Object>> result = contactService.getAllContacts();

        assertEquals(1, result.size());
    }

    @Test
    void getContactById_Success() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));

        Map<String, Object> result = contactService.getContactById(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("id"));
    }

    @Test
    void respondToContact_Success() {
        when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));
        when(contactRepository.save(any(Contact.class))).thenReturn(testContact);

        ApiMessage result = contactService.respondToContact(1L, "Thank you for contacting us");

        assertNotNull(result);
        assertEquals("Response sent successfully", result.message());
    }

    @Test
    void deleteContact_Success() {
        doNothing().when(contactRepository).deleteById(1L);

        ApiMessage result = contactService.deleteContact(1L);

        assertNotNull(result);
        assertEquals("Contact deleted successfully", result.message());
        verify(contactRepository).deleteById(1L);
    }

    // ==================== NEGATIVE TESTS ====================

    @Test
    void submitContact_NullName_ThrowsException() {
        ContactRequest request = new ContactRequest(
            null,
            "test@example.com",
            null,
            "Test",
            "Test message"
        );

        assertThrows(IllegalArgumentException.class, () -> contactService.submitContact(request, testUser));
    }

    @Test
    void submitContact_NullEmail_ThrowsException() {
        ContactRequest request = new ContactRequest(
            "Test",
            null,
            null,
            "Test",
            "Test message"
        );

        assertThrows(IllegalArgumentException.class, () -> contactService.submitContact(request, testUser));
    }

    @Test
    void submitContact_NullMessage_ThrowsException() {
        ContactRequest request = new ContactRequest(
            "Test",
            "test@example.com",
            null,
            "Test",
            null
        );

        assertThrows(IllegalArgumentException.class, () -> contactService.submitContact(request, testUser));
    }

    @Test
    void getContactById_NotFound_ThrowsException() {
        when(contactRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> contactService.getContactById(999L));
    }

    @Test
    void respondToContact_NotFound_ThrowsException() {
        when(contactRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> contactService.respondToContact(999L, "Response"));
    }

    // ==================== BOUNDARY TESTS ====================

    @Test
    void submitContact_MaxNameLength_Success() {
        String longName = "a".repeat(100);
        ContactRequest request = new ContactRequest(
            longName,
            "test@example.com",
            null,
            "Test",
            "Test message"
        );

        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        ApiMessage result = contactService.submitContact(request, testUser);

        assertNotNull(result);
    }

    @Test
    void submitContact_MaxPhoneLength_Success() {
        String longPhone = "+91-12345678901234567890";
        ContactRequest request = new ContactRequest(
            "Test",
            "test@example.com",
            longPhone,
            "Test",
            "Test message"
        );

        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        ApiMessage result = contactService.submitContact(request, testUser);

        assertNotNull(result);
    }

    // ==================== EDGE CASES ====================

    @Test
    void getUserInquiries_NoContacts_ReturnsEmptyList() {
        when(contactRepository.findByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(contactRepository.findByUserIsNullAndEmailIgnoreCaseOrderByCreatedAtDesc("test@example.com")).thenReturn(List.of());

        List<Map<String, Object>> result = contactService.getUserInquiries(testUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserInquiries_MultipleContacts_ReturnsAll() {
        Contact contact2 = new Contact();
        contact2.setId(2L);
        contact2.setUser(testUser);
        contact2.setName("Test User 2");
        contact2.setEmail("test@example.com");
        contact2.setPhone("1234567890");
        contact2.setStatus(ContactStatus.PENDING);
        contact2.setSubject("Test Subject");
        contact2.setMessage("Test Message");
        contact2.setCreatedAt(LocalDateTime.now());
        
        when(contactRepository.findByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testContact, contact2));
        when(contactRepository.findByUserIsNullAndEmailIgnoreCaseOrderByCreatedAtDesc("test@example.com")).thenReturn(List.of());

        List<Map<String, Object>> result = contactService.getUserInquiries(testUser);

        assertEquals(2, result.size());
    }

    @Test
    void getUserInquiries_IncludesLegacyEmailMatchedContacts() {
        Contact legacyContact = new Contact();
        legacyContact.setId(2L);
        legacyContact.setUser(null);
        legacyContact.setName("Test User");
        legacyContact.setEmail("TEST@example.com");
        legacyContact.setPhone("1234567890");
        legacyContact.setStatus(ContactStatus.REPLIED);
        legacyContact.setSubject("Legacy Subject");
        legacyContact.setMessage("Legacy Message");
        legacyContact.setResponse("Legacy reply");
        legacyContact.setCreatedAt(LocalDateTime.now().plusMinutes(1));

        when(contactRepository.findByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testContact));
        when(contactRepository.findByUserIsNullAndEmailIgnoreCaseOrderByCreatedAtDesc("test@example.com")).thenReturn(List.of(legacyContact));
        when(contactRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Map<String, Object>> result = contactService.getUserInquiries(testUser);

        assertEquals(2, result.size());
        assertEquals(List.of(2L, 1L), result.stream().map(item -> (Long) item.get("id")).sorted(Comparator.reverseOrder()).toList());
        assertEquals("Legacy reply", result.stream().filter(item -> Long.valueOf(2L).equals(item.get("id"))).findFirst().orElseThrow().get("adminReply"));
        verify(contactRepository).saveAll(any());
    }


    @Test
    void respondToContact_AlreadyResponded_UpdatesResponse() {
        testContact.setResponse("Previous response");
        testContact.setStatus(ContactStatus.REPLIED);
        
        when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiMessage result = contactService.respondToContact(1L, "New response");

        assertNotNull(result);
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void getContactById_IncludesAllFields() {
        testContact.setResponse("Test response");
        testContact.setStatus(ContactStatus.REPLIED);
        
        when(contactRepository.findById(1L)).thenReturn(Optional.of(testContact));

        Map<String, Object> result = contactService.getContactById(1L);

        assertEquals(1L, result.get("id"));
        assertEquals("Test User", result.get("name"));
        assertEquals("test@example.com", result.get("email"));
        assertEquals("+91-1234567890", result.get("phone"));
        assertEquals("General Inquiry", result.get("subject"));
        assertEquals("I have a question about vaccination", result.get("message"));
        assertEquals("Test response", result.get("response"));
        assertEquals("REPLIED", result.get("status"));
    }

    @Test
    void submitContact_WithSpecialCharacters_Success() {
        ContactRequest request = new ContactRequest(
            "Test <script>User</script>",
            "test@example.com",
            null,
            "Test & Inquiry",
            "Message with 'quotes' and \"double quotes\""
        );

        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        ApiMessage result = contactService.submitContact(request, testUser);

        assertNotNull(result);
    }
}
