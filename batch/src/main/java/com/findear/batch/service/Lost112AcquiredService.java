package com.findear.batch.service;

import com.findear.batch.Lost112BoardListReqDto;
import com.findear.batch.domain.Lost112Acquired;
import com.findear.batch.dto.Lost112AcquiredDto;
import com.findear.batch.repository.Lost112AcquiredRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Transactional
@Service
public class Lost112AcquiredService {

    private final Lost112AcquiredRepository lost112AcquiredRepository;

    public List<Lost112AcquiredDto> findListByAtcId(Lost112BoardListReqDto reqDto) {
        List<String> atcIdList = reqDto.getAtcIdList();
        List<Lost112Acquired> boardDatas = lost112AcquiredRepository.findByAtcIdIn(atcIdList);

        return boardDatas.stream()
                .map(Lost112AcquiredDto::of)
                .toList();
    }

    public long getNumOfLost112Acquired() {
        return lost112AcquiredRepository.count();
    }
}
