package com.findear.stubbatchserver.service;

import com.findear.stubbatchserver.repository.Lost112AcquiredRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Transactional
@Service
public class MatchingService {

    private final Lost112AcquiredRepository lost112AcquiredRepository;

    public long getNumOfLost112Acquired() {
        return lost112AcquiredRepository.count();
    }
}
