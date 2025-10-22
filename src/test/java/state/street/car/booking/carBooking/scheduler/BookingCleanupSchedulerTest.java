package state.street.car.booking.carBooking.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import state.street.car.booking.carBooking.entity.Booking;
import state.street.car.booking.carBooking.entity.Car;
import state.street.car.booking.carBooking.entity.User;
import state.street.car.booking.carBooking.enums.CarType;
import state.street.car.booking.carBooking.repository.BookingRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCleanupSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingCleanupScheduler cleanupScheduler;

    private Car testCar;
    private User testUser;

    @BeforeEach
    void setUp() {
        testCar = new Car();
        testCar.setId(1L);
        testCar.setRegistrationNumber("TEST-001");
        testCar.setType(CarType.SEDAN);
        testCar.setCostPerDay(new BigDecimal("50.00"));
        testCar.setCapacity(4);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }

    @Test
    void testCleanupExpiredBookings_WithExpiredBookings() {
        // Create an expired booking (ended 2 days ago)
        Booking expiredBooking = new Booking();
        expiredBooking.setId(1L);
        expiredBooking.setCar(testCar);
        expiredBooking.setUser(testUser);
        expiredBooking.setBookingDate(LocalDateTime.now().minusDays(5));
        expiredBooking.setDuration(2); // Ended 3 days ago
        expiredBooking.setCreatedAt(LocalDateTime.now().minusDays(5));

        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(expiredBooking));

        cleanupScheduler.cleanupExpiredBookings();

        verify(bookingRepository, times(1)).delete(expiredBooking);
    }

    @Test
    void testCleanupExpiredBookings_WithActiveBookings() {
        // Create an active booking (ends tomorrow)
        Booking activeBooking = new Booking();
        activeBooking.setId(1L);
        activeBooking.setCar(testCar);
        activeBooking.setUser(testUser);
        activeBooking.setBookingDate(LocalDateTime.now());
        activeBooking.setDuration(2); // Ends tomorrow
        activeBooking.setCreatedAt(LocalDateTime.now());

        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(activeBooking));

        cleanupScheduler.cleanupExpiredBookings();

        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    void testCleanupExpiredBookings_WithMixedBookings() {
        // Create an expired booking
        Booking expiredBooking = new Booking();
        expiredBooking.setId(1L);
        expiredBooking.setCar(testCar);
        expiredBooking.setUser(testUser);
        expiredBooking.setBookingDate(LocalDateTime.now().minusDays(10));
        expiredBooking.setDuration(3); // Ended 7 days ago
        expiredBooking.setCreatedAt(LocalDateTime.now().minusDays(10));

        // Create an active booking
        Booking activeBooking = new Booking();
        activeBooking.setId(2L);
        activeBooking.setCar(testCar);
        activeBooking.setUser(testUser);
        activeBooking.setBookingDate(LocalDateTime.now());
        activeBooking.setDuration(5); // Ends in 5 days
        activeBooking.setCreatedAt(LocalDateTime.now());

        when(bookingRepository.findAll()).thenReturn(Arrays.asList(expiredBooking, activeBooking));

        cleanupScheduler.cleanupExpiredBookings();

        verify(bookingRepository, times(1)).delete(expiredBooking);
        verify(bookingRepository, never()).delete(activeBooking);
    }

    @Test
    void testCleanupExpiredBookings_WithNoBookings() {
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        cleanupScheduler.cleanupExpiredBookings();

        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    void testManualCleanup_ReturnsCorrectCount() {
        // Create two expired bookings
        Booking expiredBooking1 = new Booking();
        expiredBooking1.setId(1L);
        expiredBooking1.setCar(testCar);
        expiredBooking1.setUser(testUser);
        expiredBooking1.setBookingDate(LocalDateTime.now().minusDays(10));
        expiredBooking1.setDuration(3);
        expiredBooking1.setCreatedAt(LocalDateTime.now().minusDays(10));

        Booking expiredBooking2 = new Booking();
        expiredBooking2.setId(2L);
        expiredBooking2.setCar(testCar);
        expiredBooking2.setUser(testUser);
        expiredBooking2.setBookingDate(LocalDateTime.now().minusDays(5));
        expiredBooking2.setDuration(2);
        expiredBooking2.setCreatedAt(LocalDateTime.now().minusDays(5));

        when(bookingRepository.findAll()).thenReturn(Arrays.asList(expiredBooking1, expiredBooking2));

        int deletedCount = cleanupScheduler.cleanupExpiredBookingsManually();

        assertEquals(2, deletedCount);
        verify(bookingRepository, times(2)).delete(any(Booking.class));
    }

    @Test
    void testManualCleanup_WithNoExpiredBookings() {
        // Create an active booking
        Booking activeBooking = new Booking();
        activeBooking.setId(1L);
        activeBooking.setCar(testCar);
        activeBooking.setUser(testUser);
        activeBooking.setBookingDate(LocalDateTime.now());
        activeBooking.setDuration(5);
        activeBooking.setCreatedAt(LocalDateTime.now());

        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(activeBooking));

        int deletedCount = cleanupScheduler.cleanupExpiredBookingsManually();

        assertEquals(0, deletedCount);
        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    void testCleanupExpiredBookings_BookingEndingYesterday() {
        // Create a booking that ended yesterday
        Booking bookingEndedYesterday = new Booking();
        bookingEndedYesterday.setId(1L);
        bookingEndedYesterday.setCar(testCar);
        bookingEndedYesterday.setUser(testUser);
        bookingEndedYesterday.setBookingDate(LocalDateTime.now().minusDays(2));
        bookingEndedYesterday.setDuration(1); // Ended yesterday
        bookingEndedYesterday.setCreatedAt(LocalDateTime.now().minusDays(2));

        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(bookingEndedYesterday));

        cleanupScheduler.cleanupExpiredBookings();

        // Should be deleted since it ended yesterday
        verify(bookingRepository, times(1)).delete(bookingEndedYesterday);
    }
}

