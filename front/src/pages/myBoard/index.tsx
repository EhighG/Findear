import { ListType, getAcquisitions, getLosts } from "@/entities";
import { FindearStamp, Text, cls, useMemberStore } from "@/shared";
import dayjs from "dayjs";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import Swal from "sweetalert2";

const MyBoard = () => {
  const { state } = useLocation();
  const { member } = useMemberStore();
  const navigate = useNavigate();
  const [boardList, setBoardList] = useState<ListType[]>([]);

  useEffect(() => {
    if (state) {
      dataFetching(state.boardType);
    }
  }, [state]);

  const dataFetching = (board: string) => {
    if (board === "습득물") {
      getAcquisitions(
        { memberId: member.memberId, pageNo: 1, sortBy: "date" },
        ({ data }) => {
          setBoardList(data.result?.boardList);
        },
        (error) => {
          console.log(error);
        }
      );
      return;
    }

    if (board === "분실물") {
      getLosts(
        { memberId: member.memberId, pageNo: 1, sortBy: "date" },
        ({ data }) => {
          console.log(data);
          setBoardList(data.result?.boardList);
        },
        (error) => {
          console.log(error);
        }
      );

      return;
    }
  };

  const calculateDate = (date: string) => {
    const today = dayjs();
    const registerDate = dayjs(date);
    const AbrogateDate = registerDate.add(180, "day");
    const remainingDays = AbrogateDate.diff(today, "day");

    return remainingDays;
  };

  return (
    <div className="flex flex-col flex-1">
      <div className="flex flex-col p-2 gap-2">
        <Text className="text-[2rem] font-bold">
          내가 작성한 {state.boardType === "분실물" ? "분실물" : "습득물"}
        </Text>
        {boardList.map((list) => (
          // ListCard
          <div
            className={cls(
              "flex h-[100px] bg-white dark:bg-A706DarkGrey1 gap-2 rounded-md shadow-lg border border-A706LightGrey2",
              list.status === "DONE" ? "opacity-60" : ""
            )}
            onClick={() => {
              navigate(
                `${
                  state.boardType === "분실물"
                    ? "/lostItemDetail/"
                    : "/foundItemDetail/"
                }${list.boardId}`
              );
            }}
          >
            <div className="flex size-[100px] bg-A706Blue2">
              <img
                src={list.thumbnailUrl}
                alt={list.productName}
                className="object-fill w-full h-full rounded-md"
              />
            </div>
            <div className="flex flex-col p-1 w-full">
              <div className="flex justify-between p-1">
                {list.status === "DONE" ? (
                  <FindearStamp className="right-16" />
                ) : (
                  ""
                )}

                <Text className="bg-A706Blue3 rounded-lg px-1 self-start">
                  {list.isLost ? "분실물" : "습득물"}
                </Text>
                {state.boardType === "습득물" ? (
                  <>
                    <Text className="flex font-bold">
                      <>
                        {list.status === "ONGOING"
                          ? "의무 보관 기간 " +
                            calculateDate(
                              list.acquiredAt ?? String(new Date())
                            ) +
                            "일"
                          : ""}
                        <img
                          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Information.png"
                          alt="Information"
                          width="25"
                          height="25"
                          className="z-[1]"
                          onClick={(e) => {
                            e.stopPropagation();
                            Swal.fire({
                              title: "의무 보관 기간",
                              text: "상법 제152조에 따르면 공중접객업자는 업장내 유실물 보관 책임이 있으면 손망실시 손해를 배상할 책임이 있습니다, 해당기한은 유실물 법에 따라 최대 180일의 책임이 있습니다.",
                              icon: "info",
                              confirmButtonText: "확인",
                            });
                          }}
                        />
                      </>
                    </Text>
                  </>
                ) : (
                  ""
                )}
              </div>

              <div className="flex justify-between">
                <Text className="text-[1rem] font-bold">
                  {list.productName}
                </Text>
                <Text>{list.category ?? "분류중"} </Text>
              </div>
              <div className="flex justify-between">
                <Text>
                  {state.boardType === "습득물"
                    ? list.agency?.name ?? "미설정"
                    : list.suspiciousPlace ?? "미분류"}
                </Text>
                <Text>
                  {state.boardType === "습득물"
                    ? list.acquiredAt ?? "미분류"
                    : list.lostAt ?? "미분류"}
                </Text>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default MyBoard;
