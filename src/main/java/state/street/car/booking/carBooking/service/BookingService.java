package state.street.car.booking.carBooking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import state.street.car.booking.carBooking.dto.BookingDTO;
import state.street.car.booking.carBooking.dto.BookingRequest;
import state.street.car.booking.carBooking.entity.Booking;
import state.street.car.booking.carBooking.entity.Car;
import state.street.car.booking.carBooking.entity.User;
import state.street.car.booking.carBooking.exception.BookingConflictException;
import state.street.car.booking.carBooking.exception.ResourceNotFoundException;
import state.street.car.booking.carBooking.repository.BookingRepository;
import state.street.car.booking.carBooking.repository.CarRepository;
import state.street.car.booking.carBooking.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    
    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BookingDTO> getBookingsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.findBookingsByDateRange(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public BookingDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return convertToDTO(booking);
    }
    
    public List<BookingDTO> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BookingDTO> getBookingsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return bookingRepository.findByUserId(user.getId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<BookingDTO> getBookingsByCarId(Long carId) {
        return bookingRepository.findByCarId(carId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public BookingDTO createBooking(BookingRequest request, String username) {
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + request.getCarId()));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        LocalDateTime endDate = request.getBookingDate().plusDays(request.getDuration());
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
                request.getCarId(),
                request.getBookingDate(),
                endDate
        );
        
        if (!conflictingBookings.isEmpty()) {
            throw new BookingConflictException(
                    "Car is not available for the requested period. " +
                    "There are " + conflictingBookings.size() + " conflicting booking(s)."
            );
        }

        Booking booking = new Booking(car, user, request.getBookingDate(), request.getDuration());
        Booking savedBooking = bookingRepository.save(booking);
        
        return convertToDTO(savedBooking);
    }
    
    public BookingDTO updateBooking(Long id, BookingRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (!booking.getCar().getId().equals(request.getCarId())) {
            Car newCar = carRepository.findById(request.getCarId())
                    .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + request.getCarId()));
            booking.setCar(newCar);
        }

        LocalDateTime endDate = request.getBookingDate().plusDays(request.getDuration());
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
                request.getCarId(),
                request.getBookingDate(),
                endDate
        ).stream()
         .filter(b -> !b.getId().equals(id))
         .collect(Collectors.toList());
        
        if (!conflictingBookings.isEmpty()) {
            throw new BookingConflictException(
                    "Car is not available for the requested period. " +
                    "There are " + conflictingBookings.size() + " conflicting booking(s)."
            );
        }
        
        booking.setBookingDate(request.getBookingDate());
        booking.setDuration(request.getDuration());
        
        Booking updatedBooking = bookingRepository.save(booking);
        return convertToDTO(updatedBooking);
    }
    
    public void deleteBooking(Long id, String username) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        User requestingUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        boolean isOwner = booking.getUser().getId().equals(requestingUser.getId());
        boolean isAdmin = requestingUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new IllegalStateException("You can only cancel your own bookings");
        }

        bookingRepository.deleteById(id);
    }
    
    public boolean isCarAvailable(Long carId, LocalDateTime startDate, Integer duration) {
        LocalDateTime endDate = startDate.plusDays(duration);
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
                carId,
                startDate,
                endDate
        );
        return conflictingBookings.isEmpty();
    }
    
    private BookingDTO convertToDTO(Booking booking) {
        return new BookingDTO(
                booking.getId(),
                booking.getCar().getId(),
                booking.getUser().getId(),
                booking.getCar().getRegistrationNumber(),
                booking.getUser().getUsername(),
                booking.getBookingDate(),
                booking.getDuration(),
                booking.getCreatedAt()
        );
    }
}

