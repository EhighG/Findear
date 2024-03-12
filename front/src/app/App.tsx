import { DarkThemeToggle, Flowbite } from "flowbite-react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { Boarding, Signup, Signin } from "@/pages";
import { Header } from "@/widgets";
import "./index.css";

const App = () => {
  return (
    <Flowbite>
      <div className="Container">
        <DarkThemeToggle className="absolute right-0" />
        <Header />
        <Router>
          <Routes>
            <Route path="/" element={<Boarding />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/signin" element={<Signin />} />
          </Routes>
        </Router>
      </div>
    </Flowbite>
  );
};

export default App;
