import { DarkThemeToggle, Flowbite } from "flowbite-react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { Boarding } from "@/pages";
import { Header } from "@/widgets";
import "./index.css";

const App = () => {
  return (
    <Flowbite>
      <div className="w-screen h-screen dark:bg-slate-800">
        <DarkThemeToggle className="absolute right-0" />
        <Header />
        <Router>
          <Routes>
            <Route path="/" element={<Boarding />} />
          </Routes>
        </Router>
      </div>
    </Flowbite>
  );
};

export default App;
