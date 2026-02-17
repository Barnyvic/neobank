package com.vaultpay.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String description;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LedgerEntry> entries = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void addEntry(LedgerEntry entry) {
        entries.add(entry);
        entry.setJournalEntry(this);
    }
}
