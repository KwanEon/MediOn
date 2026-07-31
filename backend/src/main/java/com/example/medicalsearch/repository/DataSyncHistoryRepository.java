package com.example.medicalsearch.repository;

import com.example.medicalsearch.entity.DataSyncHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSyncHistoryRepository extends JpaRepository<DataSyncHistory, Long> {

    List<DataSyncHistory> findTop12ByOrderBySyncedAtDescIdDesc();
}
