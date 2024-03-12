const AllFoundItemListTab = () => {
  return (
    <>
      <div>
        <div className="text-xs">전체 습득물</div>
        <div className="bg-blue-700 h-[2px]"></div>
        <hr className="main-tab-hr" />
      </div>
    </>
  );
};

const AllLostItemListTab = () => {
  return (
    <>
      <div>
        <div className="text-xs">전체 분실물</div>
        <div className="bg-transparent h-[2px]"></div>
        <hr className="main-tab-hr" />
        <hr />
      </div>
    </>
  );
};

const MatchedFoundItemListTab = () => {
  return (
    <>
      <div>
        <div className="text-xs">매칭 된 습득물</div>
        <div className="bg-transparent h-[2px]"></div>
        <hr className="main-tab-hr" />
      </div>
    </>
  );
};

const MyFoundItemListTab = () => {
  return (
    <>
      <div>
        <div className="text-xs">나의 습득물</div>
        <div className="bg-transparent h-[2px]"></div>
        <hr className="main-tab-hr" />
      </div>
    </>
  );
};

const MyLostItemListTab = () => {
  return (
    <>
      <div>
        <div className="text-xs">나의 분실물</div>
        <div className="bg-transparent h-[2px]"></div>
        <hr className="main-tab-hr" />
      </div>
    </>
  );
};

export {
  AllFoundItemListTab,
  AllLostItemListTab,
  MatchedFoundItemListTab,
  MyFoundItemListTab,
  MyLostItemListTab,
};
