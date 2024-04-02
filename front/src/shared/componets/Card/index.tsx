import { Text } from "@/shared";

type CardProps = {
  image: string;
  title: string;
  locate: string;
  date: string;
  isLost: boolean;
  category: string;
  onClick?: () => void;
};

const Card = ({
  image,
  title,
  locate,
  date,
  onClick,
  category,
  isLost,
}: CardProps) => {
  const noImage =
    "https://www.lost112.go.kr/lostnfs/images/sub/img02_no_img.gif";
  const noImageReplace: { [key: string]: string } = {
    가방: "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Handbag.png",
    귀금속:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Ring.png",
    도서용품:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Green%20Book.png",
    서류: "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Card%20Index%20Dividers.png",
    산업용품:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Toolbox.png",
    쇼핑백:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Shopping%20Bags.png",
    스포츠용품:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Activities/Badminton.png",
    악기: "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Guitar.png",
    유가증권:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Activities/Ticket.png",
    의류: "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Coat.png",
    자동차:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Pickup%20Truck.png",
    전자기기:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Headphone.png",
    지갑: "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Purse.png",
    증명서:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Identification%20Card.png",
    컴퓨터:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Laptop.png",
    카드: "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Credit%20Card.png",
    현금: "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Money%20Bag.png",
    휴대폰:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Mobile%20Phone.png",
    기타물품:
      "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Link.png",
  };

  const getImageLink = () => {
    if (noImageReplace[category]) {
      return noImageReplace[category];
    }
    return "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Magnifying%20Glass%20Tilted%20Right.png";
  };
  return (
    <div
      className="flex flex-col h-[230px] rounded-lg border-2 border-A706LightGrey2 dark:border-A706Grey2 w-full shadow-md cursor-pointer"
      onClick={onClick}
    >
      <div className="w-full h-[70%] rounded-t-2xl border-b-2 border-border-A706LightGrey2 dark:border-A706Grey2 ">
        <img
          src={
            image
              ? image !== noImage
                ? image
                : getImageLink()
              : getImageLink()
          }
          alt="이미지없음"
          className="w-full h-full object-fill object-center rounded-t-md"
        />
      </div>
      <div className="flex flex-col w-full h-[30%]] p-1">
        <Text className="text-[1rem] font-bold truncate"> {title} </Text>
        <Text className="text-[12px] truncate">
          {isLost ? "분실" : "습득"}위치 : {locate}
        </Text>
        <Text className="text-[12px] truncate">
          {isLost ? "분실" : "습득"}일자 : {date}
        </Text>
      </div>
    </div>
  );
};

export default Card;
