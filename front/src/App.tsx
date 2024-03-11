import { DarkThemeToggle, Flowbite } from "flowbite-react";
import 분실물 from '@/widgets/components/분실물'

const App = () => {
  return (
    <Flowbite>
      <div className="w-screen h-screen dark:bg-slate-800">
        <DarkThemeToggle />
        <분실물/>
      </div>
    </Flowbite>
  );
};

export default App;
