import {
  AllFoundItemListTab,
  AllLostItemListTab,
  MatchedFoundItemListTab,
  MyFoundItemListTab,
  MyLostItemListTab,
} from "@/widgets";

const MainListTabs = () => {
  return (
    <>
      <div className="flex flex-row justify-start">
        <AllFoundItemListTab />
        <AllLostItemListTab />
        <MatchedFoundItemListTab />
        <MyFoundItemListTab />
        <MyLostItemListTab />
      </div>
    </>
  );
};

export default MainListTabs;
