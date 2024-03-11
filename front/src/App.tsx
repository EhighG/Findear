import { DarkThemeToggle, Flowbite } from "flowbite-react";

const App = () => {
  return (
    <Flowbite>
      <div className="w-screen h-screen dark:bg-slate-800">
        <DarkThemeToggle />
      </div>
    </Flowbite>
  );
};

export default App;
