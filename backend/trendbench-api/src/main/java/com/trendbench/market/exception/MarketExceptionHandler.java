package com.trendbench.market.exception;

import com.trendbench.global.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MarketExceptionHandler {

    @ExceptionHandler(MarketStatisticsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(MarketStatisticsNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("MARKET_STATISTICS_NOT_FOUND", e.getMessage()));
    }
}