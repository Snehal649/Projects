package com.fintrack.repo;

import com.fintrack.model.Transaction;
import com.fintrack.model.TxType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("""
        select t from Transaction t
        where t.user.id = :uid and t.date between :from and :to
          and (:cat is null or t.category.id = :cat)
          and (:acc is null or t.account.id = :acc)
          and (:type is null or t.type = :type)
        order by t.date desc, t.id desc
        """)
    Page<Transaction> search(@Param("uid") Long uid, @Param("from") LocalDate from, @Param("to") LocalDate to,
                             @Param("cat") Long cat, @Param("acc") Long acc, @Param("type") TxType type,
                             Pageable pageable);

    List<Transaction> findByUserIdAndTypeAndDateBetween(Long userId, TxType type, LocalDate from, LocalDate to);

    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate from, LocalDate to);

    @Query("""
        select coalesce(sum(t.amount), 0) from Transaction t
        where t.user.id = :uid and t.type = :type and t.date between :from and :to
        """)
    BigDecimal sum(@Param("uid") Long uid, @Param("type") TxType type,
                   @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        select t.category.id, t.category.name, sum(t.amount) from Transaction t
        where t.user.id = :uid and t.type = :type and t.date between :from and :to
        group by t.category.id, t.category.name order by sum(t.amount) desc
        """)
    List<Object[]> sumByCategory(@Param("uid") Long uid, @Param("type") TxType type,
                                 @Param("from") LocalDate from, @Param("to") LocalDate to);
}
