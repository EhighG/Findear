import { MainList, MainNavBar } from "@/widgets";

const Main = () => {
  return (
    <>
      <div className="h-[640px]">
        <MainNavBar />
        <MainList />
      </div>
    </>
  );
};

export default Main;
