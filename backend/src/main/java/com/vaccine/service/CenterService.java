package com.vaccine.service;

import com.vaccine.dto.CenterDTO;
import com.vaccine.entity.VaccinationCenter;
import com.vaccine.repository.VaccinationCenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CenterService {
    
    @Autowired
    private VaccinationCenterRepository centerRepository;
    
    public List<CenterDTO> getAllCenters() {
        return centerRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public List<CenterDTO> getActiveCenters() {
        return centerRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    public CenterDTO getCenterById(Long id) {
        VaccinationCenter center = centerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Center not found"));
        return mapToDTO(center);
    }
    
    public List<CenterDTO> getCentersByCity(String city) {
        return centerRepository.findByCity(city).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CenterDTO createCenter(CenterDTO dto) {
        VaccinationCenter center = new VaccinationCenter();
        center.setName(dto.getName());
        center.setAddress(dto.getAddress());
        center.setCity(dto.getCity());
        center.setState(dto.getState());
        center.setPincode(dto.getPincode());
        center.setLatitude(dto.getLatitude());
        center.setLongitude(dto.getLongitude());
        center.setPhone(dto.getPhone());
        center.setEmail(dto.getEmail());
        center.setCapacityPerDay(dto.getCapacityPerDay());
        center.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        center = centerRepository.save(center);
        return mapToDTO(center);
    }
    
    @Transactional
    public CenterDTO updateCenter(Long id, CenterDTO dto) {
        VaccinationCenter center = centerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Center not found"));
        
        center.setName(dto.getName());
        center.setAddress(dto.getAddress());
        center.setCity(dto.getCity());
        center.setState(dto.getState());
        center.setPincode(dto.getPincode());
        center.setLatitude(dto.getLatitude());
        center.setLongitude(dto.getLongitude());
        center.setPhone(dto.getPhone());
        center.setEmail(dto.getEmail());
        center.setCapacityPerDay(dto.getCapacityPerDay());
        if (dto.getIsActive() != null) {
            center.setIsActive(dto.getIsActive());
        }
        
        center = centerRepository.save(center);
        return mapToDTO(center);
    }
    
    @Transactional
    public void deleteCenter(Long id) {
        VaccinationCenter center = centerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Center not found"));
        center.setIsActive(false);
        centerRepository.save(center);
    }
    
    private CenterDTO mapToDTO(VaccinationCenter center) {
        CenterDTO dto = new CenterDTO();
        dto.setId(center.getId());
        dto.setName(center.getName());
        dto.setAddress(center.getAddress());
        dto.setCity(center.getCity());
        dto.setState(center.getState());
        dto.setPincode(center.getPincode());
        dto.setLatitude(center.getLatitude());
        dto.setLongitude(center.getLongitude());
        dto.setPhone(center.getPhone());
        dto.setEmail(center.getEmail());
        dto.setCapacityPerDay(center.getCapacityPerDay());
        dto.setIsActive(center.getIsActive());
        return dto;
    }
}
