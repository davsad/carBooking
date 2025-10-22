package state.street.car.booking.carBooking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import state.street.car.booking.carBooking.dto.BookingDTO;
import state.street.car.booking.carBooking.dto.BookingRequest;
import state.street.car.booking.carBooking.entity.Booking;
import state.street.car.booking.carBooking.entity.Car;
import state.street.car.booking.carBooking.entity.Role;
import state.street.car.booking.carBooking.entity.User;
import state.street.car.booking.carBooking.enums.CarType;
import state.street.car.booking.carBooking.exception.BookingConflictException;
import state.street.car.booking.carBooking.exception.ResourceNotFoundException;
import state.street.car.booking.carBooking.repository.BookingRepository;
import state.street.car.booking.carBooking.repository.CarRepository;
import state.street.car.booking.carBooking.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private Car testCar;
    private User testUser;
    private Booking testBooking;
    private BookingRequest bookingRequest;

    @BeforeEach
    void setUp() {
        testCar = new Car("TEST-001", CarType.SEDAN, new BigDecimal("50.00"), 4);
        testCar.setId(1L);

        testUser = new User("testuser", "password", "test@example.com");
        testUser.setId(1L);

        LocalDateTime bookingDate = LocalDateTime.now().plusDays(1);
        testBooking = new Booking(testCar, testUser, bookingDate, 3);
        testBooking.setId(1L);
        testBooking.setCreatedAt(LocalDateTime.now());

        bookingRequest = new BookingRequest(1L, bookingDate, 3);
    }

    @Test
    void testCreateBooking_Success() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(new ArrayList<>());
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        BookingDTO result = bookingService.createBooking(bookingRequest, "testuser");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getCarId());
        assertEquals("testuser", result.getUsername());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_CarNotFound() {
        when(carRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.createBooking(bookingRequest, "testuser");
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_UserNotFound() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.createBooking(bookingRequest, "testuser");
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testCreateBooking_ConflictingBooking() {
        List<Booking> conflictingBookings = new ArrayList<>();
        conflictingBookings.add(testBooking);

        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(conflictingBookings);

        assertThrows(BookingConflictException.class, () -> {
            bookingService.createBooking(bookingRequest, "testuser");
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void testGetBookingById_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        BookingDTO result = bookingService.getBookingById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("TEST-001", result.getCarRegistrationNumber());
    }

    @Test
    void testGetBookingById_NotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.getBookingById(1L);
        });
    }

    @Test
    void testGetAllBookings() {
        List<Booking> bookings = new ArrayList<>();
        bookings.add(testBooking);

        when(bookingRepository.findAll()).thenReturn(bookings);

        List<BookingDTO> result = bookingService.getAllBookings();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void testDeleteBooking_Success() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(testUser);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        bookingService.deleteBooking(1L, "testuser");

        verify(bookingRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteBooking_NotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            bookingService.deleteBooking(1L, "testuser");
        });

        verify(bookingRepository, never()).deleteById(any());
    }

    @Test
    void testIsCarAvailable_Available() {
        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(new ArrayList<>());

        boolean result = bookingService.isCarAvailable(1L, LocalDateTime.now().plusDays(1), 3);

        assertTrue(result);
    }

    @Test
    void testIsCarAvailable_NotAvailable() {
        List<Booking> conflictingBookings = new ArrayList<>();
        conflictingBookings.add(testBooking);

        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(conflictingBookings);

        boolean result = bookingService.isCarAvailable(1L, LocalDateTime.now().plusDays(1), 3);

        assertFalse(result);
    }

    @Test
    void testDeleteBooking_UserCanDeleteOwnBooking() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(testUser);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        bookingService.deleteBooking(1L, "testuser");

        verify(bookingRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteBooking_AdminCanDeleteAnyBooking() {
        User adminUser = new User("admin", "password", "admin@example.com");
        adminUser.setId(2L);
        Role adminRole = new Role("ROLE_ADMIN");
        adminUser.setRoles(Set.of(adminRole));

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(testUser); // Booking belongs to testUser

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        bookingService.deleteBooking(1L, "admin"); // Admin deleting another user's booking

        verify(bookingRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteBooking_UserCannotDeleteOthersBooking() {
        User otherUser = new User("otheruser", "password", "other@example.com");
        otherUser.setId(2L);
        Role userRole = new Role("ROLE_USER");
        otherUser.setRoles(Set.of(userRole));

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(testUser);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));

        assertThrows(IllegalStateException.class, () -> {
            bookingService.deleteBooking(1L, "otheruser"); // otheruser trying to delete testuser's booking
        });

        verify(bookingRepository, never()).deleteById(any());
    }

}

