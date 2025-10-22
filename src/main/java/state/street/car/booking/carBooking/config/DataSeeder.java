package state.street.car.booking.carBooking.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import state.street.car.booking.carBooking.entity.Car;
import state.street.car.booking.carBooking.entity.Role;
import state.street.car.booking.carBooking.entity.User;
import state.street.car.booking.carBooking.enums.CarType;
import state.street.car.booking.carBooking.repository.CarRepository;
import state.street.car.booking.carBooking.repository.RoleRepository;
import state.street.car.booking.carBooking.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        Role adminRole = new Role("ROLE_ADMIN");
        Role userRole = new Role("ROLE_USER");
        
        roleRepository.save(adminRole);
        roleRepository.save(userRole);

        User admin = new User("admin", passwordEncoder.encode("admin123"), "admin@carbooking.com");
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);
        
        User user1 = new User("john", passwordEncoder.encode("user123"), "john@example.com");
        user1.setRoles(Set.of(userRole));
        userRepository.save(user1);
        
        User user2 = new User("jane", passwordEncoder.encode("user123"), "jane@example.com");
        user2.setRoles(Set.of(userRole));
        userRepository.save(user2);
        
        User adminUser = new User("manager", passwordEncoder.encode("manager123"), "manager@carbooking.com");
        adminUser.setRoles(Set.of(adminRole, userRole));
        userRepository.save(adminUser);

        carRepository.save(new Car("SED-001", CarType.SEDAN, new BigDecimal("50.00"), 4));
        carRepository.save(new Car("SED-002", CarType.SEDAN, new BigDecimal("55.00"), 5));
        carRepository.save(new Car("SED-003", CarType.SEDAN, new BigDecimal("60.00"), 5));

        carRepository.save(new Car("VAN-001", CarType.VAN, new BigDecimal("80.00"), 8));
        carRepository.save(new Car("VAN-002", CarType.VAN, new BigDecimal("85.00"), 9));
        carRepository.save(new Car("VAN-003", CarType.VAN, new BigDecimal("90.00"), 10));

        carRepository.save(new Car("SUV-001", CarType.SUV, new BigDecimal("70.00"), 5));
        carRepository.save(new Car("SUV-002", CarType.SUV, new BigDecimal("72.00"), 5));
        carRepository.save(new Car("SUV-003", CarType.SUV, new BigDecimal("75.00"), 6));
        carRepository.save(new Car("SUV-004", CarType.SUV, new BigDecimal("75.00"), 6));
        carRepository.save(new Car("SUV-005", CarType.SUV, new BigDecimal("78.00"), 7));
        carRepository.save(new Car("SUV-006", CarType.SUV, new BigDecimal("78.00"), 7));
        carRepository.save(new Car("SUV-007", CarType.SUV, new BigDecimal("80.00"), 7));
        carRepository.save(new Car("SUV-008", CarType.SUV, new BigDecimal("82.00"), 7));
        carRepository.save(new Car("SUV-009", CarType.SUV, new BigDecimal("85.00"), 8));
        carRepository.save(new Car("SUV-010", CarType.SUV, new BigDecimal("90.00"), 8));
        
        System.out.println("Database seeded successfully!");
        System.out.println("Admin credentials: admin/admin123");
        System.out.println("Manager credentials: manager/manager123");
        System.out.println("User credentials: john/user123, jane/user123");
    }
}

