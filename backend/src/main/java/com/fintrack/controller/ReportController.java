package com.fintrack.controller;

import com.fintrack.model.Transaction;
import com.fintrack.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final TransactionRepository txRepo;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal Long uid,
                                            @RequestParam LocalDate from, @RequestParam LocalDate to) {
        List<Transaction> list = txRepo.findByUserIdAndDateBetween(uid, from, to);
        list.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        StringBuilder sb = new StringBuilder("Date,Type,Category,Account,Merchant,Amount,Note\n");
        for (Transaction t : list) {
            sb.append(t.getDate()).append(',').append(t.getType()).append(',')
              .append(csv(t.getCategory().getName())).append(',').append(csv(t.getAccount().getName())).append(',')
              .append(csv(t.getMerchant())).append(',').append(t.getAmount()).append(',')
              .append(csv(t.getNote())).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=fintrack-" + from + "-to-" + to + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String csv(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.startsWith("=") || v.startsWith("+") || v.startsWith("-") || v.startsWith("@")) v = "'" + v; // CSV injection guard
        return "\"" + v + "\"";
    }
}
