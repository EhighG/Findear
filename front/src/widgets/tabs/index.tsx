import { CustomTab, Text } from "@/shared";

type props = {
  text: string;
  index?: number;
  selectedIndex?: number;
  onClick?: () => void;
};

const ListTab = ({ text, index, selectedIndex, onClick }: props) => {
  return (
    <button type="button" onClick={onClick}>
      <>
        <Text
          children={text}
          className={
            (index === selectedIndex ? "text-A706CheryBlue" : "text-black") +
            " mx-3 pb-2"
          }
        />
        <div
          className={
            (index === selectedIndex ? "bg-A706CheryBlue" : "bg-transparent") +
            " h-[2px]"
          }
        />
      </>
    </button>
  );
};

const TitleTab = ({ text }: props) =>
  CustomTab({
    childrens: [
      <Text children={text} className={"text-black text-xs mx-3 pb-2"} />,
      <div className={"bg-black h-[2px]"} />,
    ],
  });

export { ListTab, TitleTab };
