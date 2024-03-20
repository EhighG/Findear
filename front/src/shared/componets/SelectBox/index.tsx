import { Select } from "flowbite-react";

type selectType = {
  options: Array<{ value: string }>;
  id: string;
  className?: string;
  required?: boolean;
};

const SelectBox = ({ options, id, required, className }: selectType) => {
  return (
    <div className={className ? className : ""}>
      <Select id={id} required={required ?? false}>
        <option value="" selected>
          전체
        </option>
        {options.map((option) => (
          <option>{option.value}</option>
        ))}
      </Select>
    </div>
  );
};

export default SelectBox;
