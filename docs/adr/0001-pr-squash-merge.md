# master 반영은 PR + squash merge

복구 1차(D-34)까지는 PR 없이 로컬에서 각 커밋에 `Related to #N`을 rebase로 붙여(D-39) master에 fast-forward 병합했다. 이후 작업부터는 브랜치마다 PR을 만들어 master에 **squash merge**한다. squash 커밋 본문에만 `Related to #N`을 넣고, 세부 커밋은 PR과 원격 작업 브랜치에 남긴다. README 리팩토링 규칙의 의도(이슈 번호는 master 커밋에만 붙여 이슈 창에 중복 노출을 막고, 세부 작업 기록은 원격 브랜치에 보존)를 PR 흐름에서 그대로 지키는 방식이라 이렇게 정했다.

Supersedes D-34(master 반영 절차 중 "PR은 쓰지 않음"·fast-forward 병합 부분), D-39(`tools/git/add-issue-ref.sh` rebase — 더 쓰지 않음).

## Considered Options

- **Merge commit**: 브랜치 커밋이 이슈 번호 없이 master에 들어가 "master 커밋에는 이슈 번호" 규칙과 어긋난다.
- **Rebase merge**: GitHub이 커밋을 다시 쓰므로 이슈 참조를 push 전에 브랜치 커밋에 붙여야 하고, 그러면 이슈 창에 같은 커밋이 중복으로 보인다.

## Consequences

- master 히스토리는 PR당 커밋 1개가 된다.
- PR 생성·병합에 gh 토큰의 Pull requests: Read and write, Contents: Read and write 권한이 필요하다 (`08-work-plan.md` U-10).
