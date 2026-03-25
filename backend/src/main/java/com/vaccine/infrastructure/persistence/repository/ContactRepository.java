package com.vaccine.infrastructure.persistence.repository;

import com.vaccine.domain.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<Contact> findByUserIsNullAndEmailIgnoreCaseOrderByCreatedAtDesc(String email);
    List<Contact> findAllByOrderByCreatedAtDescIdDesc();
    void deleteByUser_Id(Long userId);
}
