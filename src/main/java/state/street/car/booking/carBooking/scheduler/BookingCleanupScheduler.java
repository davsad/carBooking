package state.street.car.booking.carBooking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import state.street.car.booking.carBooking.entity.Booking;
import state.street.car.booking.carBooking.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to automatically clean up expired bookings.
 * Runs daily at 2 AM to delete bookings where booking_date + duration < current_time
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupScheduler {
    
    private final BookingRepository bookingRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredBookings() {
        log.info("Starting scheduled cleanup of expired bookings...");

        LocalDateTime now = LocalDateTime.now();
        List<Booking> allBookings = bookingRepository.findAll();

        int deletedCount = 0;
        for (Booking booking : allBookings) {
            LocalDateTime bookingEndDate = booking.getBookingEndDate();

            if (bookingEndDate.isBefore(now)) {
                extracted(booking, bookingEndDate);
                bookingRepository.delete(booking);
                deletedCount++;
            }
        }

        log.info("Completed cleanup. Deleted {} expired booking(s)", deletedCount);
    }

    /**
     * Manual cleanup method that can be called on-demand
     * Useful for testing or manual triggers
     */
    @Transactional
    public int cleanupExpiredBookingsManually() {
        log.info("Manual cleanup of expired bookings triggered...");

        LocalDateTime now = LocalDateTime.now();
        List<Booking> allBookings = bookingRepository.findAll();

        int deletedCount = 0;
        for (Booking booking : allBookings) {
            LocalDateTime bookingEndDate = booking.getBookingEndDate();

            if (bookingEndDate.isBefore(now)) {
                extracted(booking, bookingEndDate);
                bookingRepository.delete(booking);
                deletedCount++;
            }
        }

        log.info("Manual cleanup completed. Deleted {} expired booking(s)", deletedCount);
        return deletedCount;
    }

    private void extracted(Booking booking, LocalDateTime bookingEndDate) {
        log.info("Deleting expired booking: ID={}, Car={}, EndDate={}",
            booking.getId(),
            booking.getCar().getRegistrationNumber(),
                bookingEndDate);
    }
}

