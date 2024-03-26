import SearchIcon from "@mui/icons-material/Search";
import Inventory2OutlinedIcon from "@mui/icons-material/Inventory2Outlined";
import NavigateNextOutlinedIcon from "@mui/icons-material/NavigateNextOutlined";
import KeyboardDoubleArrowRightIcon from "@mui/icons-material/KeyboardDoubleArrowRight";
import { ListTab } from "@/widgets";
import { CustomButton, SSEConnect, Text, useMemberStore } from "@/shared";
import { useEffect, useState } from "react";

const Main = () => {
  let eventSource: EventSource;

  const SSEConnection = () => {
    try {
      eventSource = SSEConnect();
    } catch (error) {
      console.log(error);
    }

    eventSource.onopen = () => {
      console.log("Server Sent Event 연결이 열렸습니다.");
    };

    eventSource.onerror = () => {
      console.log("Server Sent Event 오류");
    };

    eventSource.addEventListener("message", (event) => {
      const data = JSON.parse(event.data);
      console.log(data);
    });
  };

  useEffect(() => {
    SSEConnection();
  }, []);

  const { member } = useMemberStore();
  const [selectedIndex, setSelectedIndex] = useState<number>(0);

  const renderMainButton = () => {
    return member.role === "MANAGER" ? (
      <CustomButton className="bg-A706CheryBlue rounded-3xl flex flex-col justify-around p-5 my-5">
        <>
          <div className="flex w-full">
            <Inventory2OutlinedIcon
              className="self-center text-A706Blue"
              fontSize="large"
            />
            <Text className="font-bold text-xl text-A706Blue ml-5 text-start">
              누군가 물건을 놓고 갔나요?
            </Text>
          </div>
          <Text className="text-sm text-A706Blue text-right w-full mt-10">
            <>
              빠르게 돌려주기
              <KeyboardDoubleArrowRightIcon fontSize="small" />
            </>
          </Text>
        </>
      </CustomButton>
    ) : (
      <CustomButton className="bg-A706Yellow rounded-3xl flex flex-col justify-around p-5 my-5">
        <>
          <div className="flex w-full">
            <SearchIcon className="self-center" fontSize="large" />
            <Text className="font-bold text-xl text-A706DarkGrey2 ml-5 text-start">
              소중한 물건을 잃어버리셨나요?
            </Text>
          </div>
          <Text className="text-sm text-A706DarkGrey2 text-right w-full mt-10">
            <>
              찾아보기
              <KeyboardDoubleArrowRightIcon fontSize="small" />
            </>
          </Text>
        </>
      </CustomButton>
    );
  };

  useEffect(() => {
    console.log("index selected: " + selectedIndex);
  }, [selectedIndex]);

  return (
    <div className="flex flex-col self-center w-[360px]">
      <div className="flex flex-col my-5">
        {renderMainButton()}
        <Text className="text-center text-A706DarkGrey1 text-sm">더보기</Text>
      </div>
      <div className="flex flex-row justify-around w-full">
        <ListTab
          text={"실시간 습득물"}
          index={0}
          selectedIndex={selectedIndex}
          onClick={() => setSelectedIndex(0)}
        />
        <ListTab
          text={"?"}
          index={1}
          selectedIndex={selectedIndex}
          onClick={() => setSelectedIndex(1)}
        />
        <ListTab
          text={"?"}
          index={2}
          selectedIndex={selectedIndex}
          onClick={() => setSelectedIndex(2)}
        />
      </div>
      <hr className="main-tab-hr" />
      <div className="py-5">
        <div className="flex flex-col rounded-lg border-2 m-3 py-3 px-3">
          <div className="flex flex-row">
            <img className="size-[64px] m-3" src="images/wallet.jpg"></img>
            <div className="text-start items-center m-5 w-full">
              <h3>갈색 지갑</h3>
            </div>
            <p className="text-left text-xs my-5 mx-3">
              습득일: 2024.03.26 16:05:01{" "}
            </p>
          </div>
          <CustomButton className="bg-A706CheryBlue rounded-lg text-white py-2 m-2">
            자세히 보기
          </CustomButton>
        </div>
        <div className="flex flex-col rounded-lg border-2 m-3 py-3 px-3">
          <div className="flex flex-row">
            <img className="size-[64px] m-3" src="images/wallet.jpg"></img>
            <div className="text-start items-center m-5 w-full">
              <h3>갈색 지갑</h3>
            </div>
            <p className="text-left text-xs my-5 mx-3">
              습득일: 2024.03.26 16:05:01{" "}
            </p>
          </div>
          <CustomButton className="bg-A706CheryBlue rounded-lg text-white py-2 m-2">
            자세히 보기
          </CustomButton>
        </div>
        <div className="flex flex-col rounded-lg border-2 m-3 py-3 px-3">
          <div className="flex flex-row">
            <img className="size-[64px] m-3" src="images/wallet.jpg"></img>
            <div className="text-start items-center m-5 w-full">
              <h3>갈색 지갑</h3>
            </div>
            <p className="text-left text-xs my-5 mx-3">
              습득일: 2024.03.26 16:05:01{" "}
            </p>
          </div>
          <CustomButton className="bg-A706CheryBlue rounded-lg text-white py-2 m-2">
            자세히 보기
          </CustomButton>
        </div>
        <div className="flex justify-end">
          <Text>전체 보기</Text>
          <NavigateNextOutlinedIcon />
        </div>
      </div>
    </div>
  );
};

export default Main;
