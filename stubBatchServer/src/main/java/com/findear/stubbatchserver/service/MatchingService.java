package com.findear.stubbatchserver.service;

import com.findear.stubbatchserver.Lost112BoardListReqDto;
import com.findear.stubbatchserver.domain.Lost112Acquired;
import com.findear.stubbatchserver.dto.Lost112AcquiredDto;
import com.findear.stubbatchserver.repository.Lost112AcquiredRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
public class MatchingService {

    private final Lost112AcquiredRepository lost112AcquiredRepository;

    public List<Lost112AcquiredDto> findLost112AcquiredListByAtcId(Lost112BoardListReqDto reqDto) {
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
