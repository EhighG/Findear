import { useMemberStore } from "@/shared";
import { ListTab } from "@/widgets";
import { useEffect, useState } from "react";

const MainListTabs = () => {
  const { member } = useMemberStore();
  const [selectedIndex, setSelectedIndex] = useState(0);

  useEffect(() => {
    console.log("index selected: " + selectedIndex);
  }, [selectedIndex]);

  if (!member) {
    return (
      <>
        <div className="flex flex-row justify-start">
          <ListTab
            text={"전체 습득물"}
            index={0}
            selectedIndex={selectedIndex}
            onClick={() => setSelectedIndex(0)}
          />
          <ListTab
            text={"전체 분실물"}
            index={1}
            selectedIndex={selectedIndex}
            onClick={() => setSelectedIndex(1)}
          />
        </div>
        <hr className="main-tab-hr" />
      </>
    );
  } else {
    if (member.role === "NORMAL") {
      return (
        <>
          <div className="flex flex-row justify-start">
            <ListTab
              text={"매칭 된 습득물"}
              index={0}
              selectedIndex={selectedIndex}
              onClick={() => setSelectedIndex(0)}
            />
            <ListTab
              text={"전체 습득물"}
              index={1}
              selectedIndex={selectedIndex}
              onClick={() => setSelectedIndex(1)}
            />
            <ListTab
              text={"나의 분실물"}
              index={2}
              selectedIndex={selectedIndex}
              onClick={() => setSelectedIndex(2)}
            />
          </div>
          <hr className="main-tab-hr" />
        </>
      );
    } else if (member.role === "MANAGER") {
      return (
        <>
          <div className="flex flex-row justify-start">
            <ListTab
              text={"나의 습득물"}
              index={0}
              selectedIndex={selectedIndex}
              onClick={() => setSelectedIndex(0)}
            />
            <ListTab
              text={"전체 분실물"}
              index={1}
              selectedIndex={selectedIndex}
              onClick={() => setSelectedIndex(1)}
            />
            <ListTab
              text={"전체 습득물"}
              index={2}
              selectedIndex={selectedIndex}
              onClick={() => setSelectedIndex(2)}
            />
          </div>
          <hr className="main-tab-hr" />
        </>
      );
    }
  }
};

export default MainListTabs;
