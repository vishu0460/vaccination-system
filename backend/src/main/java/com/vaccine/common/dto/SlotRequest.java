package com.vaccine.common.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SlotRequest {
    @Min(value = 1, message = "Drive ID must be positive")
    private Long driveId;

    @JsonAlias({"startTime", "startDateTime"})
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime startDate;

    @JsonAlias({"endTime", "endDateTime"})
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime endDate;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @JsonAlias({"date"})
    private String date;

    @JsonAlias({"time"})
    private String time;

    @JsonAlias({"available"})
    private Boolean available;

    @AssertTrue(message = "End date must be after start date")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }
}
