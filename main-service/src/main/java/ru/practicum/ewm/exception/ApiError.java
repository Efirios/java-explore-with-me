package ru.practicum.ewm.exception;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiError {

    private List<String> errors;

    private String message;

    private String reason;

    private String status;

    private String timestamp;
}
