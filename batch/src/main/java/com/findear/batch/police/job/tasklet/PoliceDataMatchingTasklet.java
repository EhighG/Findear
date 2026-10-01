package com.findear.batch.police.job.tasklet;

import com.findear.batch.ours.dto.LostBoardMatchingDto;
import com.findear.batch.ours.job.tasklet.LostBoardMatchingTasklet;
import com.findear.batch.ours.repository.LostBoardRepository;
import com.findear.batch.ours.service.PoliceMatchingService;
import org.springframework.stereotype.Component;

/** policeJob의 매칭 스텝: 대상 분실물마다 Lost112 습득물 매칭 ({@link LostBoardMatchingTasklet} 참고) */
@Component
public class PoliceDataMatchingTasklet extends LostBoardMatchingTasklet {

    private final PoliceMatchingService policeMatchingService;

    public PoliceDataMatchingTasklet(LostBoardRepository lostBoardRepository, PoliceMatchingService policeMatchingService) {
        super(lostBoardRepository);
        this.policeMatchingService = policeMatchingService;
    }

    @Override
    protected String kind() {
        return "lost112";
    }

    @Override
    protected void matchOne(LostBoardMatchingDto lostBoard) {
        policeMatchingService.match(lostBoard);
    }
}
