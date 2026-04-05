package com.financeapp.config;

import com.financeapp.entity.Transaction;
import com.financeapp.entity.User;
import com.financeapp.enums.Role;
import com.financeapp.enums.TransactionType;
import com.financeapp.enums.UserStatus;
import com.financeapp.repository.TransactionRepository;
import com.financeapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedTransactions();
    }

    private void seedUsers() {
        if (userRepository.existsByEmail("admin@finance.com")) return;

        User admin = userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@finance.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        userRepository.save(User.builder()
                .name("Jane Analyst")
                .email("analyst@finance.com")
                .password(passwordEncoder.encode("analyst123"))
                .role(Role.ANALYST)
                .status(UserStatus.ACTIVE)
                .build());

        userRepository.save(User.builder()
                .name("Bob Viewer")
                .email("viewer@finance.com")
                .password(passwordEncoder.encode("viewer123"))
                .role(Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .build());

        log.info("===========================================");
        log.info("Seeded default users:");
        log.info("  ADMIN   → admin@finance.com   / admin123");
        log.info("  ANALYST → analyst@finance.com / analyst123");
        log.info("  VIEWER  → viewer@finance.com  / viewer123");
        log.info("===========================================");

        seedTransactionsForUser(admin);
    }

    private void seedTransactions() {
        // Only seed if no transactions exist
        if (transactionRepository.count() > 0) return;
    }

    private void seedTransactionsForUser(User admin) {
        LocalDate now = LocalDate.now();

        Object[][] data = {
            { new BigDecimal("5000.00"), TransactionType.INCOME,  "Salary",       now.minusDays(30), "Monthly salary" },
            { new BigDecimal("1200.00"), TransactionType.EXPENSE, "Rent",         now.minusDays(28), "Monthly rent" },
            { new BigDecimal("350.00"),  TransactionType.EXPENSE, "Groceries",    now.minusDays(20), "Weekly groceries" },
            { new BigDecimal("800.00"),  TransactionType.INCOME,  "Freelance",    now.minusDays(15), "Web design project" },
            { new BigDecimal("120.00"),  TransactionType.EXPENSE, "Utilities",    now.minusDays(12), "Electricity bill" },
            { new BigDecimal("5000.00"), TransactionType.INCOME,  "Salary",       now.minusDays(1),  "Monthly salary" },
            { new BigDecimal("45.00"),   TransactionType.EXPENSE, "Subscriptions",now.minusDays(5),  "Streaming services" },
            { new BigDecimal("600.00"),  TransactionType.EXPENSE, "Dining",       now.minusDays(8),  "Team dinner" },
        };

        for (Object[] row : data) {
            transactionRepository.save(Transaction.builder()
                    .amount((BigDecimal) row[0])
                    .type((TransactionType) row[1])
                    .category((String) row[2])
                    .date((LocalDate) row[3])
                    .notes((String) row[4])
                    .createdBy(admin)
                    .build());
        }

        log.info("Seeded {} sample transactions.", data.length);
    }
}
