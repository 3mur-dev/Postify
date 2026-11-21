package com.omar.postify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


public record PeriodRange (
         LocalDateTime start,
         LocalDateTime end
){}


