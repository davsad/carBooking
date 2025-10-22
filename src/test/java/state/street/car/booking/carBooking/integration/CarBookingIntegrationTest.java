package state.street.car.booking.carBooking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import state.street.car.booking.carBooking.dto.BookingRequest;
import state.street.car.booking.carBooking.dto.CarDTO;
import state.street.car.booking.carBooking.entity.Booking;
import state.street.car.booking.carBooking.entity.Car;
import state.street.car.booking.carBooking.entity.Role;
import state.street.car.booking.carBooking.entity.User;
import state.street.car.booking.carBooking.enums.CarType;
import state.street.car.booking.carBooking.repository.BookingRepository;
import state.street.car.booking.carBooking.repository.CarRepository;
import state.street.car.booking.carBooking.repository.RoleRepository;
import state.street.car.booking.carBooking.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CarBookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Car testCar;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create roles
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

        // Create test user
        testUser = new User("integrationuser", "password", "integration@test.com");
        testUser.setRoles(Set.of(userRole));
        testUser = userRepository.save(testUser);

        // Create test car
        testCar = new Car("INT-TEST-001", CarType.SEDAN, new BigDecimal("50.00"), 4);
        testCar = carRepository.save(testCar);
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testGetAllCars() throws Exception {
        LocalDateTime startDate = LocalDateTime.now();
        mockMvc.perform(get("/api/cars")
                        .param("startDate", startDate.toString())
                        .param("duration", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testGetCarById() throws Exception {
        mockMvc.perform(get("/api/cars/" + testCar.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationNumber").value("INT-TEST-001"))
                .andExpect(jsonPath("$.type").value("SEDAN"));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testGetCarsByType() throws Exception {
        LocalDateTime startDate = LocalDateTime.now();
        mockMvc.perform(get("/api/cars")
                        .param("type", "SEDAN")
                        .param("startDate", startDate.toString())
                        .param("duration", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void testCreateCar_AsAdmin() throws Exception {
        CarDTO newCar = new CarDTO(null, "NEW-CAR-001", CarType.SUV, new BigDecimal("70.00"), 5);

        mockMvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCar)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationNumber").value("NEW-CAR-001"))
                .andExpect(jsonPath("$.type").value("SUV"));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testCreateCar_AsUser_Forbidden() throws Exception {
        CarDTO newCar = new CarDTO(null, "NEW-CAR-002", CarType.VAN, new BigDecimal("80.00"), 8);

        mockMvc.perform(post("/api/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCar)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testCreateBooking_Success() throws Exception {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(5);
        BookingRequest request = new BookingRequest(testCar.getId(), futureDate, 3);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carId").value(testCar.getId()))
                .andExpect(jsonPath("$.duration").value(3));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testCreateBooking_CarNotFound() throws Exception {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(5);
        BookingRequest request = new BookingRequest(9999L, futureDate, 3);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testCreateConflictingBooking() throws Exception {
        // Create first booking
        LocalDateTime futureDate = LocalDateTime.now().plusDays(10);
        BookingRequest request1 = new BookingRequest(testCar.getId(), futureDate, 5);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        LocalDateTime conflictingDate = futureDate.plusDays(2);
        BookingRequest request2 = new BookingRequest(testCar.getId(), conflictingDate, 3);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testGetAvailableCars() throws Exception {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(20);
        String startDateStr = futureDate.toString();

        mockMvc.perform(get("/api/cars")
                .param("startDate", startDateStr)
                .param("duration", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void testGetAllBookings_AsAdmin() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testGetAllBookings_AsUser_Forbidden() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void testUpdateCar_AsAdmin() throws Exception {
        CarDTO updateDTO = new CarDTO(testCar.getId(), "UPDATED-001", CarType.SUV, new BigDecimal("60.00"), 5);

        mockMvc.perform(put("/api/cars/" + testCar.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationNumber").value("UPDATED-001"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void testDeleteCar_AsAdmin() throws Exception {
        Car carToDelete = new Car("DELETE-001", CarType.VAN, new BigDecimal("80.00"), 8);
        carToDelete = carRepository.save(carToDelete);

        mockMvc.perform(delete("/api/cars/" + carToDelete.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testDeleteCar_AsUser_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/cars/" + testCar.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integrationuser"))
                .andExpect(jsonPath("$.email").value("integration@test.com"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testGetAllCarsWithBookingInfo() throws Exception {
        Booking booking = new Booking();
        booking.setCar(testCar);
        booking.setUser(testUser);
        booking.setBookingDate(LocalDateTime.now());
        booking.setDuration(3);
        booking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        LocalDateTime startDate = LocalDateTime.now();
        mockMvc.perform(get("/api/cars")
                        .param("startDate", startDate.toString())
                        .param("duration", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.registrationNumber=='INT-TEST-001')].isBooked").value(true))
                .andExpect(jsonPath("$[?(@.registrationNumber=='INT-TEST-001')].bookedByUsername").value("integrationuser"));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testUserCanDeleteOwnBooking() throws Exception {
        Booking booking = new Booking();
        booking.setCar(testCar);
        booking.setUser(testUser);
        booking.setBookingDate(LocalDateTime.now());
        booking.setDuration(3);
        booking.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);

        mockMvc.perform(delete("/api/bookings/" + savedBooking.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "otheruser", authorities = {"ROLE_USER"})
    void testUserCannotDeleteOthersBooking() throws Exception {
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();
        User otherUser = new User("otheruser", "password", "other@test.com");
        otherUser.setRoles(Set.of(userRole));
        otherUser = userRepository.save(otherUser);

        Booking booking = new Booking();
        booking.setCar(testCar);
        booking.setUser(testUser);
        booking.setBookingDate(LocalDateTime.now());
        booking.setDuration(3);
        booking.setCreatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);

        mockMvc.perform(delete("/api/bookings/" + savedBooking.getId()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("You can only cancel your own bookings"));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testBookingWorkflow() throws Exception {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(30);
        String startDateStr = futureDate.toString();

        mockMvc.perform(get("/api/cars")
                .param("startDate", startDateStr)
                .param("duration", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        BookingRequest request = new BookingRequest(testCar.getId(), futureDate, 2);

        String bookingResponse = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carId").value(testCar.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(get("/api/cars")
                .param("startDate", startDateStr)
                .param("duration", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void testManualCleanupExpiredBookings() throws Exception {
        Booking expiredBooking = new Booking();
        expiredBooking.setCar(testCar);
        expiredBooking.setUser(testUser);
        expiredBooking.setBookingDate(LocalDateTime.now().minusDays(10));
        expiredBooking.setDuration(3); // Ended 7 days ago
        expiredBooking.setCreatedAt(LocalDateTime.now().minusDays(10));
        bookingRepository.save(expiredBooking);

        mockMvc.perform(post("/api/bookings/cleanup-expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cleanup completed successfully"))
                .andExpect(jsonPath("$.deletedBookings").value(1));
    }

    @Test
    @WithMockUser(username = "user", authorities = {"ROLE_USER"})
    void testManualCleanup_AsUser_Forbidden() throws Exception {
        mockMvc.perform(post("/api/bookings/cleanup-expired"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void testManualCleanup_NoExpiredBookings() throws Exception {
        Booking activeBooking = new Booking();
        activeBooking.setCar(testCar);
        activeBooking.setUser(testUser);
        activeBooking.setBookingDate(LocalDateTime.now());
        activeBooking.setDuration(5); // Ends in 5 days
        activeBooking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(activeBooking);

        mockMvc.perform(post("/api/bookings/cleanup-expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cleanup completed successfully"))
                .andExpect(jsonPath("$.deletedBookings").value(0));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testSearchCarsWithDateRange_NoConflicts() throws Exception {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(30);
        String startDateStr = futureDate.toString();

        mockMvc.perform(get("/api/cars")
                .param("startDate", startDateStr)
                .param("duration", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.registrationNumber=='INT-TEST-001')].isBooked").value(false));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testSearchCarsWithDateRange_WithConflicts() throws Exception {
        LocalDateTime bookingStart = LocalDateTime.now().plusDays(10);
        Booking booking = new Booking();
        booking.setCar(testCar);
        booking.setUser(testUser);
        booking.setBookingDate(bookingStart);
        booking.setDuration(5);
        booking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        LocalDateTime searchStart = LocalDateTime.now().plusDays(12);
        String startDateStr = searchStart.toString();

        mockMvc.perform(get("/api/cars")
                .param("startDate", startDateStr)
                .param("duration", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.registrationNumber=='INT-TEST-001')].isBooked").value(true))
                .andExpect(jsonPath("$[?(@.registrationNumber=='INT-TEST-001')].bookedByUsername").value("integrationuser"));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testSearchCarsWithDateRange_NoOverlap() throws Exception {
        LocalDateTime bookingStart = LocalDateTime.now().plusDays(10);
        Booking booking = new Booking();
        booking.setCar(testCar);
        booking.setUser(testUser);
        booking.setBookingDate(bookingStart);
        booking.setDuration(3);
        booking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        LocalDateTime searchStart = LocalDateTime.now().plusDays(20); // No overlap
        String startDateStr = searchStart.toString();

        mockMvc.perform(get("/api/cars")
                .param("startDate", startDateStr)
                .param("duration", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.registrationNumber=='INT-TEST-001')].isBooked").value(false));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testCreateBooking_OverlapDetection() throws Exception {
        LocalDateTime existingBookingStart = LocalDateTime.now().plusDays(10);
        Booking existingBooking = new Booking();
        existingBooking.setCar(testCar);
        existingBooking.setUser(testUser);
        existingBooking.setBookingDate(existingBookingStart);
        existingBooking.setDuration(5);
        existingBooking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(existingBooking);

        LocalDateTime newBookingStart = LocalDateTime.now().plusDays(12); // Overlaps
        BookingRequest request = new BookingRequest(testCar.getId(), newBookingStart, 2);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not available")));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testCreateBooking_NoOverlap() throws Exception {
        LocalDateTime existingBookingStart = LocalDateTime.now().plusDays(10);
        Booking existingBooking = new Booking();
        existingBooking.setCar(testCar);
        existingBooking.setUser(testUser);
        existingBooking.setBookingDate(existingBookingStart);
        existingBooking.setDuration(3);
        existingBooking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(existingBooking);

        LocalDateTime newBookingStart = LocalDateTime.now().plusDays(20); // No overlap
        BookingRequest request = new BookingRequest(testCar.getId(), newBookingStart, 2);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carId").value(testCar.getId()));
    }

    @Test
    @WithMockUser(username = "integrationuser", authorities = {"ROLE_USER"})
    void testSearchCarsByType_WithDateRange() throws Exception {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(25);
        String startDateStr = futureDate.toString();

        mockMvc.perform(get("/api/cars")
                .param("type", "SEDAN")
                .param("startDate", startDateStr)
                .param("duration", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].type").value("SEDAN"));
    }
}

