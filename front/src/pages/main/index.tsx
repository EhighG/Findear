import { MainList, MainNavBar } from "@/widgets";

const Main = () => {
  return (
    <div className="flex flex-col">
      <MainNavBar />
      <MainList />
    </div>
  );
};

export default Main;
