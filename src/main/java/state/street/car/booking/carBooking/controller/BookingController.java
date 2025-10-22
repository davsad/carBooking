package state.street.car.booking.carBooking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import state.street.car.booking.carBooking.dto.BookingDTO;
import state.street.car.booking.carBooking.dto.BookingRequest;
import state.street.car.booking.carBooking.scheduler.BookingCleanupScheduler;
import state.street.car.booking.carBooking.service.BookingService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingCleanupScheduler cleanupScheduler;
    
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<BookingDTO>> getAllBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<BookingDTO> bookings;

        // If date range is provided, filter bookings
        if (startDate != null && endDate != null) {
            bookings = bookingService.getBookingsByDateRange(startDate, endDate);
        } else {
            bookings = bookingService.getAllBookings();
        }

        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getBookingById(@PathVariable Long id) {
        BookingDTO booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }
    
    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingDTO>> getMyBookings(Authentication authentication) {
        String username = authentication.getName();
        List<BookingDTO> bookings = bookingService.getBookingsByUsername(username);
        return ResponseEntity.ok(bookings);
    }
    
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<BookingDTO> createBooking(
            @RequestBody BookingRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        BookingDTO createdBooking = bookingService.createBooking(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BookingDTO> updateBooking(
            @PathVariable Long id,
            @RequestBody BookingRequest request) {
        BookingDTO updatedBooking = bookingService.updateBooking(id, request);
        return ResponseEntity.ok(updatedBooking);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<Void> deleteBooking(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        bookingService.deleteBooking(id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin endpoint to manually trigger cleanup of expired bookings
     */
    @PostMapping("/cleanup-expired")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupExpiredBookings() {
        int deletedCount = cleanupScheduler.cleanupExpiredBookingsManually();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cleanup completed successfully");
        response.put("deletedBookings", deletedCount);

        return ResponseEntity.ok(response);
    }
}

