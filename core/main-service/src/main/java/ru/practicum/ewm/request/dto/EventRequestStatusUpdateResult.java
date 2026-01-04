package ru.practicum.ewm.request.dto;

import lombok.Data;

import java.util.List;

@Data
public class EventRequestStatusUpdateResult {
    private List<ParticipationRequestResponse> confirmedRequests;
    private List<ParticipationRequestResponse> rejectedRequests;
}