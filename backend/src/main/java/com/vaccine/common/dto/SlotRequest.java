package com.vaccine.common.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private Long driveId;

    @JsonAlias({"startTime", "startDateTime"})
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime startDate;

    @JsonAlias({"endTime", "endDateTime"})
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm[:ss]")
    private LocalDateTime endDate;

    private Integer capacity;

    @JsonAlias({"date"})
    private String date;

    @JsonAlias({"time"})
    private String time;

    @JsonAlias({"available"})
    private Boolean available;
}
