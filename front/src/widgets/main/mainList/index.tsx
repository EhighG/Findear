import { Member } from "@/entities";
import { useMemberStore } from "@/shared";
import { MainListTabs, MainListItem } from "@/widgets";

const MainList = () => {
  const { member } = useMemberStore();
  let listSize = 10;
  let items = [];
  const item = MainListItem({
    id: 0,
    imageUrl: "url",
    title: "제목",
    category: "카테고리",
    nickname: "닉네임",
    acquiredAt: new Date("2024/03/13"),
  });

  const requestItems = (member: Member | null) => {
    if (!member) {
      for (let i = 0; i < listSize; i++) {
        items.push(item);
      }
      return items;
    } else {
      if (member.role === "NORMAL") {
        for (let i = 0; i < listSize; i++) {
          items.push(item);
        }
        return items;
      } else if (member.role === "MANAGER") {
        for (let i = 0; i < listSize; i++) {
          items.push(item);
        }
        return items;
      }
    }
  };

  return (
    <>
      <div className="flex flex-col flex-1">
        <MainListTabs />
        <div className="flex flex-col overflow-hidden hover:overflow-scroll hover:overflow-x-hidden">
          {requestItems(member)?.map((result) => result) ??
            "에러가 발생했습니다!"}
        </div>
      </div>
    </>
  );
};

export default MainList;
