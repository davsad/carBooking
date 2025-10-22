package state.street.car.booking.carBooking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import state.street.car.booking.carBooking.dto.CarDTO;
import state.street.car.booking.carBooking.entity.Booking;
import state.street.car.booking.carBooking.entity.Car;
import state.street.car.booking.carBooking.entity.User;
import state.street.car.booking.carBooking.enums.CarType;
import state.street.car.booking.carBooking.exception.ResourceNotFoundException;
import state.street.car.booking.carBooking.repository.BookingRepository;
import state.street.car.booking.carBooking.repository.CarRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private CarService carService;

    private Car testCar;
    private CarDTO testCarDTO;

    @BeforeEach
    void setUp() {
        testCar = new Car("TEST-001", CarType.SEDAN, new BigDecimal("50.00"), 4);
        testCar.setId(1L);

        testCarDTO = new CarDTO(1L, "TEST-001", CarType.SEDAN, new BigDecimal("50.00"), 4);
    }

    @Test
    void testGetAllCars() {
        List<Car> cars = new ArrayList<>();
        cars.add(testCar);

        when(carRepository.findAll()).thenReturn(cars);
        when(bookingRepository.findActiveBookingsForCar(any(), any())).thenReturn(new ArrayList<>());

        List<CarDTO> result = carService.getAllCars();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TEST-001", result.get(0).getRegistrationNumber());
        verify(carRepository, times(1)).findAll();
    }

    @Test
    void testGetCarsByType() {
        List<Car> cars = new ArrayList<>();
        cars.add(testCar);

        when(carRepository.findByType(CarType.SEDAN)).thenReturn(cars);
        when(bookingRepository.findActiveBookingsForCar(any(), any())).thenReturn(new ArrayList<>());

        List<CarDTO> result = carService.getCarsByType(CarType.SEDAN);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CarType.SEDAN, result.get(0).getType());
        verify(carRepository, times(1)).findByType(CarType.SEDAN);
    }

    @Test
    void testGetCarById_Success() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));

        CarDTO result = carService.getCarById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("TEST-001", result.getRegistrationNumber());
        verify(carRepository, times(1)).findById(1L);
    }

    @Test
    void testGetCarById_NotFound() {
        when(carRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            carService.getCarById(1L);
        });
    }

    @Test
    void testCreateCar() {
        when(carRepository.save(any(Car.class))).thenReturn(testCar);

        CarDTO result = carService.createCar(testCarDTO);

        assertNotNull(result);
        assertEquals("TEST-001", result.getRegistrationNumber());
        assertEquals(CarType.SEDAN, result.getType());
        verify(carRepository, times(1)).save(any(Car.class));
    }

    @Test
    void testUpdateCar_Success() {
        CarDTO updateDTO = new CarDTO(1L, "TEST-002", CarType.SUV, new BigDecimal("70.00"), 5);

        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(carRepository.save(any(Car.class))).thenReturn(testCar);

        CarDTO result = carService.updateCar(1L, updateDTO);

        assertNotNull(result);
        verify(carRepository, times(1)).findById(1L);
        verify(carRepository, times(1)).save(any(Car.class));
    }

    @Test
    void testUpdateCar_NotFound() {
        when(carRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            carService.updateCar(1L, testCarDTO);
        });

        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    void testDeleteCar_Success() {
        when(carRepository.existsById(1L)).thenReturn(true);
        when(bookingRepository.findActiveBookingsForCar(any(), any())).thenReturn(new ArrayList<>());

        carService.deleteCar(1L);

        verify(carRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteCar_NotFound() {
        when(carRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            carService.deleteCar(1L);
        });

        verify(carRepository, never()).deleteById(any());
    }

    @Test
    void testGetAvailableCars() {
        List<Car> cars = new ArrayList<>();
        cars.add(testCar);

        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        Integer duration = 3;

        when(carRepository.findAvailableCars(any(), any())).thenReturn(cars);

        List<CarDTO> result = carService.getAvailableCars(startDate, duration);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(carRepository, times(1)).findAvailableCars(any(), any());
    }

    @Test
    void testGetAvailableCarsByType() {
        List<Car> cars = new ArrayList<>();
        cars.add(testCar);

        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        Integer duration = 3;

        when(carRepository.findAvailableCarsByType(any(), any(), any())).thenReturn(cars);

        List<CarDTO> result = carService.getAvailableCarsByType(CarType.SEDAN, startDate, duration);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CarType.SEDAN, result.get(0).getType());
        verify(carRepository, times(1)).findAvailableCarsByType(any(), any(), any());
    }

    @Test
    void testGetAllCarsForPeriod_NoConflicts() {
        List<Car> cars = new ArrayList<>();
        cars.add(testCar);

        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        Integer duration = 3;

        when(carRepository.findAll()).thenReturn(cars);
        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(new ArrayList<>());

        List<CarDTO> result = carService.getAllCarsForPeriod(startDate, duration);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsBooked());
        verify(carRepository, times(1)).findAll();
        verify(bookingRepository, times(1)).findConflictingBookings(any(), any(), any());
    }

    @Test
    void testGetAllCarsForPeriod_WithConflicts() {
        List<Car> cars = new ArrayList<>();
        cars.add(testCar);

        User user = new User("testuser", "password", "test@example.com");
        user.setId(1L);

        Booking conflictingBooking = new Booking();
        conflictingBooking.setId(1L);
        conflictingBooking.setCar(testCar);
        conflictingBooking.setUser(user);
        conflictingBooking.setBookingDate(LocalDateTime.now().plusDays(2));
        conflictingBooking.setDuration(2);
        conflictingBooking.setCreatedAt(LocalDateTime.now());

        List<Booking> conflicts = new ArrayList<>();
        conflicts.add(conflictingBooking);

        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        Integer duration = 3;

        when(carRepository.findAll()).thenReturn(cars);
        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(conflicts);

        List<CarDTO> result = carService.getAllCarsForPeriod(startDate, duration);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsBooked());
        assertEquals("testuser", result.get(0).getBookedByUsername());
        verify(carRepository, times(1)).findAll();
        verify(bookingRepository, times(1)).findConflictingBookings(any(), any(), any());
    }

    @Test
    void testGetCarsByTypeForPeriod_NoConflicts() {
        List<Car> cars = new ArrayList<>();
        cars.add(testCar);

        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        Integer duration = 3;

        when(carRepository.findByType(CarType.SEDAN)).thenReturn(cars);
        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(new ArrayList<>());

        List<CarDTO> result = carService.getCarsByTypeForPeriod(CarType.SEDAN, startDate, duration);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsBooked());
        assertEquals(CarType.SEDAN, result.get(0).getType());
        verify(carRepository, times(1)).findByType(CarType.SEDAN);
        verify(bookingRepository, times(1)).findConflictingBookings(any(), any(), any());
    }

    @Test
    void testGetCarsByTypeForPeriod_WithConflicts() {
        List<Car> cars = new ArrayList<>();
        cars.add(testCar);

        User user = new User("testuser", "password", "test@example.com");
        user.setId(1L);

        Booking conflictingBooking = new Booking();
        conflictingBooking.setId(1L);
        conflictingBooking.setCar(testCar);
        conflictingBooking.setUser(user);
        conflictingBooking.setBookingDate(LocalDateTime.now().plusDays(2));
        conflictingBooking.setDuration(2);
        conflictingBooking.setCreatedAt(LocalDateTime.now());

        List<Booking> conflicts = new ArrayList<>();
        conflicts.add(conflictingBooking);

        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        Integer duration = 3;

        when(carRepository.findByType(CarType.SEDAN)).thenReturn(cars);
        when(bookingRepository.findConflictingBookings(any(), any(), any())).thenReturn(conflicts);

        List<CarDTO> result = carService.getCarsByTypeForPeriod(CarType.SEDAN, startDate, duration);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsBooked());
        assertEquals("testuser", result.get(0).getBookedByUsername());
        assertEquals(CarType.SEDAN, result.get(0).getType());
        verify(carRepository, times(1)).findByType(CarType.SEDAN);
        verify(bookingRepository, times(1)).findConflictingBookings(any(), any(), any());
    }
}

