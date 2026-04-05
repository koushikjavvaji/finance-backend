package com.financeapp.repository;

import com.financeapp.entity.Transaction;
import com.financeapp.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ── Single lookup (exclude soft-deleted) ──────────────────────────────────
    Optional<Transaction> findByIdAndDeletedFalse(Long id);

    // ── Paginated listing with optional filters + keyword search ─────────────
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.deleted = false
              AND (:type     IS NULL OR t.type     = :type)
              AND (:category IS NULL OR LOWER(t.category) = LOWER(:category))
              AND (:from     IS NULL OR t.date    >= :from)
              AND (:to       IS NULL OR t.date    <= :to)
              AND (:keyword  IS NULL
                   OR LOWER(t.notes)    LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(t.category) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY t.date DESC
            """)
    Page<Transaction> findWithFilters(
            @Param("type")     TransactionType type,
            @Param("category") String category,
            @Param("from")     LocalDate from,
            @Param("to")       LocalDate to,
            @Param("keyword")  String keyword,
            Pageable pageable
    );

    // ── Dashboard aggregates ──────────────────────────────────────────────────
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.deleted = false AND t.type = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("""
            SELECT t.category, COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.deleted = false
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> sumByCategory();

    @Query("""
            SELECT FUNCTION('YEAR', t.date)  AS yr,
                   FUNCTION('MONTH', t.date) AS mo,
                   t.type,
                   COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.deleted = false
              AND t.date >= :from
            GROUP BY FUNCTION('YEAR', t.date), FUNCTION('MONTH', t.date), t.type
            ORDER BY yr, mo
            """)
    List<Object[]> monthlyTrends(@Param("from") LocalDate from);

    // ── Recent activity ───────────────────────────────────────────────────────
    List<Transaction> findTop10ByDeletedFalseOrderByCreatedAtDesc();

    // ── Distinct categories (for filter dropdowns) ────────────────────────────
    @Query("SELECT DISTINCT t.category FROM Transaction t WHERE t.deleted = false ORDER BY t.category")
    List<String> findDistinctCategories();
}
