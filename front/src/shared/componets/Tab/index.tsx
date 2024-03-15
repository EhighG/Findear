import { SyntheticEvent } from "react";

type tabProps = {
  className?: string;
  onClick?: (event: SyntheticEvent) => void;
  childrens: (string | JSX.Element)[];
};

const CustomTab = ({ childrens, className, onClick }: tabProps) => {
  return (
    <button type="button" onClick={onClick} className={className}>
      {childrens.map((child) => child)}
    </button>
  );
};

export default CustomTab;
