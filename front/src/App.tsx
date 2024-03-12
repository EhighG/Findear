import { DarkThemeToggle, Flowbite } from "flowbite-react";
import LostItemButton from '@/widgets/LostItemButton'

const App = () => {
  return (
    <Flowbite>
      <div className="w-screen h-screen dark:bg-slate-800">
        <DarkThemeToggle />
        <LostItemButton/>
      </div>
    </Flowbite>
  );
};

export default App;

