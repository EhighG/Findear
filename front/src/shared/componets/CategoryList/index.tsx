type CategoryListProps = {
  setCategory: (category: string) => void;
  setModal?: (bool: boolean) => void;
  className?: string;
};

const CategoryList = ({
  setCategory,
  setModal,
  className,
}: CategoryListProps) => {
  return (
    <ul className={className ? className : ""}>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={() => {
          setCategory("");
          setModal ? setModal(false) : "";
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Magnifying%20Glass%20Tilted%20Right.png"
          alt="Magnifying Glass Tilted Right"
          width="50"
          height="50"
        />
        전체
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal ? setModal(false) : "";
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Backpack.png"
          alt="Backpack"
          width="50"
          height="50"
        />
        가방
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal ? setModal(false) : "";
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Gem%20Stone.png"
          alt="Gem Stone"
          width="50"
          height="50"
        />
        귀금속
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal ? setModal(false) : "";
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Books.png"
          alt="Books"
          width="50"
          height="50"
        />
        도서용품
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal ? setModal(false) : "";
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Card%20Index%20Dividers.png"
          alt="Card Index Dividers"
          width="50"
          height="50"
        />
        서류
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Toolbox.png"
          alt="Toolbox"
          width="50"
          height="50"
        />
        산업용품
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Shopping%20Bags.png"
          alt="Shopping Bags"
          width="50"
          height="50"
        />
        쇼핑백
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Activities/Basketball.png"
          alt="Basketball"
          width="50"
          height="50"
        />
        스포츠용품
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Violin.png"
          alt="Violin"
          width="50"
          height="50"
        />
        악기
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Activities/Ticket.png"
          alt="Ticket"
          width="50"
          height="50"
        />
        유가증권
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);

          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/T-Shirt.png"
          alt="T-Shirt"
          width="50"
          height="50"
        />
        의류
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Pickup%20Truck.png"
          alt="Pickup Truck"
          width="50"
          height="50"
        />
        자동차
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Headphone.png"
          alt="Headphone"
          width="50"
          height="50"
        />
        전자기기
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Purse.png"
          alt="Purse"
          width="50"
          height="50"
        />
        지갑
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Page%20Facing%20Up.png"
          alt="Page Facing Up"
          width="50"
          height="50"
        />
        증명서
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Laptop.png"
          alt="Laptop"
          width="50"
          height="50"
        />
        컴퓨터
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Credit%20Card.png"
          alt="Credit Card"
          width="50"
          height="50"
        />
        카드
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Money%20Bag.png"
          alt="Money Bag"
          width="50"
          height="50"
        />
        현금
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Mobile%20Phone.png"
          alt="Mobile Phone"
          width="50"
          height="50"
        />
        휴대폰
      </li>
      <li
        className="flex flex-col items-center w-full h-20 justify-center cursor-pointer"
        onClick={(e) => {
          setCategory(e.currentTarget.innerText);
          setModal && setModal(false);
        }}
      >
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Link.png"
          alt="Link"
          width="50"
          height="50"
        />
        기타물품
      </li>
    </ul>
  );
};

export default CategoryList;
