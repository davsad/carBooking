package state.street.car.booking.carBooking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import state.street.car.booking.carBooking.dto.CarDTO;
import state.street.car.booking.carBooking.entity.Booking;
import state.street.car.booking.carBooking.entity.Car;
import state.street.car.booking.carBooking.enums.CarType;
import state.street.car.booking.carBooking.exception.ResourceNotFoundException;
import state.street.car.booking.carBooking.repository.BookingRepository;
import state.street.car.booking.carBooking.repository.CarRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CarService {

    private final CarRepository carRepository;
    private final BookingRepository bookingRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    public List<CarDTO> getAllCars() {
        return carRepository.findAll().stream()
                .map(this::convertToDTOWithBookingInfo)
                .collect(Collectors.toList());
    }

    public List<CarDTO> getAllCarsSimple() {
        return carRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CarDTO> getCarsByType(CarType type) {
        return carRepository.findByType(type).stream()
                .map(this::convertToDTOWithBookingInfo)
                .collect(Collectors.toList());
    }

    public List<CarDTO> getCarsByTypeSimple(CarType type) {
        return carRepository.findByType(type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public CarDTO getCarById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + id));
        return convertToDTO(car);
    }
    
    public CarDTO createCar(CarDTO carDTO) {
        Car car = new Car(
                carDTO.getRegistrationNumber(),
                carDTO.getType(),
                carDTO.getCostPerDay(),
                carDTO.getCapacity()
        );
        Car savedCar = carRepository.save(car);
        return convertToDTO(savedCar);
    }
    
    public CarDTO updateCar(Long id, CarDTO carDTO) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + id));
        
        car.setRegistrationNumber(carDTO.getRegistrationNumber());
        car.setType(carDTO.getType());
        car.setCostPerDay(carDTO.getCostPerDay());
        car.setCapacity(carDTO.getCapacity());
        
        Car updatedCar = carRepository.save(car);
        return convertToDTO(updatedCar);
    }
    
    public void deleteCar(Long id) {
        if (!carRepository.existsById(id)) {
            throw new ResourceNotFoundException("Car not found with id: " + id);
        }

        List<Booking> activeBookings = bookingRepository.findActiveBookingsForCar(id, LocalDateTime.now());
        if (!activeBookings.isEmpty()) {
            throw new IllegalStateException("Cannot delete car with active bookings. Please cancel all bookings first.");
        }

        carRepository.deleteById(id);
    }
    
    public List<CarDTO> getAvailableCars(LocalDateTime startDate, Integer duration) {
        LocalDateTime endDate = startDate.plusDays(duration);
        return carRepository.findAvailableCars(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<CarDTO> getAvailableCarsByType(CarType type, LocalDateTime startDate, Integer duration) {
        LocalDateTime endDate = startDate.plusDays(duration);
        return carRepository.findAvailableCarsByType(type, startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CarDTO> getAllCarsWithBookingInfo() {
        return carRepository.findAll().stream()
                .map(this::convertToDTOWithBookingInfo)
                .collect(Collectors.toList());
    }

    public List<CarDTO> getCarsByTypeWithBookingInfo(CarType type) {
        return carRepository.findByType(type).stream()
                .map(this::convertToDTOWithBookingInfo)
                .collect(Collectors.toList());
    }

    public List<CarDTO> getAllCarsForPeriod(LocalDateTime startDate, Integer duration) {
        return carRepository.findAll().stream()
                .map(car -> convertToDTOWithBookingInfoForPeriod(car, startDate, duration))
                .collect(Collectors.toList());
    }

    public List<CarDTO> getCarsByTypeForPeriod(CarType type, LocalDateTime startDate, Integer duration) {
        return carRepository.findByType(type).stream()
                .map(car -> convertToDTOWithBookingInfoForPeriod(car, startDate, duration))
                .collect(Collectors.toList());
    }

    private CarDTO convertToDTO(Car car) {
        return new CarDTO(
                car.getId(),
                car.getRegistrationNumber(),
                car.getType(),
                car.getCostPerDay(),
                car.getCapacity()
        );
    }

    private CarDTO convertToDTOWithBookingInfo(Car car) {
        CarDTO dto = new CarDTO(
                car.getId(),
                car.getRegistrationNumber(),
                car.getType(),
                car.getCostPerDay(),
                car.getCapacity()
        );

        List<Booking> activeBookings = bookingRepository.findActiveBookingsForCar(car.getId(), LocalDateTime.now());

        if (!activeBookings.isEmpty()) {
            Booking currentBooking = activeBookings.get(0); // Get the earliest active booking
            dto.setIsBooked(true);
            dto.setCurrentBookingId(currentBooking.getId());
            dto.setBookedByUserId(currentBooking.getUser().getId());
            dto.setBookedByUsername(currentBooking.getUser().getUsername());
            dto.setBookingStartDate(currentBooking.getBookingDate().format(DATE_FORMATTER));
            dto.setBookingEndDate(currentBooking.getBookingEndDate().format(DATE_FORMATTER));
        } else {
            dto.setIsBooked(false);
        }

        return dto;
    }

    private CarDTO convertToDTOWithBookingInfoForPeriod(Car car, LocalDateTime startDate, Integer duration) {
        CarDTO dto = new CarDTO(
                car.getId(),
                car.getRegistrationNumber(),
                car.getType(),
                car.getCostPerDay(),
                car.getCapacity()
        );

        LocalDateTime endDate = startDate.plusDays(duration);
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(car.getId(), startDate, endDate);

        if (!conflictingBookings.isEmpty()) {
            Booking conflictingBooking = conflictingBookings.get(0); // Get the first conflicting booking
            dto.setIsBooked(true);
            dto.setCurrentBookingId(conflictingBooking.getId());
            dto.setBookedByUserId(conflictingBooking.getUser().getId());
            dto.setBookedByUsername(conflictingBooking.getUser().getUsername());
            dto.setBookingStartDate(conflictingBooking.getBookingDate().format(DATE_FORMATTER));
            dto.setBookingEndDate(conflictingBooking.getBookingEndDate().format(DATE_FORMATTER));
        } else {
            dto.setIsBooked(false);
        }

        return dto;
    }
}

