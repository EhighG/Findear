package com.findear.batch.ours.job.tasklet;

import com.findear.batch.ours.dto.LostBoardMatchingDto;
import com.findear.batch.ours.repository.LostBoardRepository;
import com.findear.batch.ours.service.FindearMatchingService;
import org.springframework.stereotype.Component;

/** findearJob의 매칭 스텝: 대상 분실물마다 Findear 습득물 매칭 ({@link LostBoardMatchingTasklet} 참고) */
@Component
public class FindearDataMatchingTasklet extends LostBoardMatchingTasklet {

    private final FindearMatchingService findearMatchingService;

    public FindearDataMatchingTasklet(LostBoardRepository lostBoardRepository, FindearMatchingService findearMatchingService) {
        super(lostBoardRepository);
        this.findearMatchingService = findearMatchingService;
    }

    @Override
    protected String kind() {
        return "findear";
    }

    @Override
    protected void matchOne(LostBoardMatchingDto lostBoard) {
        findearMatchingService.match(lostBoard);
    }
}
