#!/bin/sh
# master 반영 전, 작업 브랜치의 각 커밋 메시지 본문 끝에 이슈 참조(예: "Related to #13")를 붙인다.
# 마지막 문단이 트레일러(Co-Authored-By: 등)로만 되어 있으면 그 앞에 넣어 트레일러가 계속 인식되게 한다.
# 이미 같은 참조가 있는 커밋은 건드리지 않는다.
#
# 사용 (레포 루트, 작업 브랜치에서):
#   ISSUE_REF='Related to #13' git rebase -x 'sh tools/git/add-issue-ref.sh' master
set -eu

ref="${ISSUE_REF:-}"
case "$ref" in
  "Related to #"[0-9]*) ;;
  *) echo "ISSUE_REF must look like 'Related to #N' (got: '$ref')" >&2; exit 1 ;;
esac

msg=$(git log -1 --format=%B)
if printf '%s\n' "$msg" | grep -qxF "$ref"; then
  exit 0
fi

printf '%s\n' "$msg" | awk -v ref="$ref" '
  { line[NR] = $0 }
  END {
    n = NR
    while (n > 0 && line[n] ~ /^[ \t]*$/) n--
    last_blank = 0
    for (i = 1; i <= n; i++) if (line[i] ~ /^[ \t]*$/) last_blank = i
    trailers = (last_blank > 1)
    for (i = last_blank + 1; i <= n && trailers; i++)
      if (line[i] !~ /^[A-Za-z0-9][A-Za-z0-9-]*: [^ ]/) trailers = 0
    if (trailers) {
      for (i = 1; i < last_blank; i++) print line[i]
      print ""; print ref; print ""
      for (i = last_blank + 1; i <= n; i++) print line[i]
    } else {
      for (i = 1; i <= n; i++) print line[i]
      print ""; print ref
    }
  }' | git commit --amend --allow-empty --quiet -F -
