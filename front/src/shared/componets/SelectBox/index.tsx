import { Select } from "flowbite-react";
import { ChangeEvent } from "react";

type selectType = {
  options: Array<{ value: string }>;
  className?: string;
  onChange: (e: ChangeEvent<HTMLSelectElement>) => void;
};

const SelectBox = ({ options, className, onChange }: selectType) => {
  return (
    <div className={className ? className : ""}>
      <Select defaultValue="" onChange={onChange}>
        <option value="" disabled>
          전체
        </option>
        {options.map((option, index) => (
          <option value={option.value} key={index}>
            {option.value}
          </option>
        ))}
      </Select>
    </div>
  );
};

export default SelectBox;
