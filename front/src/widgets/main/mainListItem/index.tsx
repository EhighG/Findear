import FavoriteBorderIcon from "@mui/icons-material/FavoriteBorder";
import { CustomButton, Text } from "@/shared";
import { SyntheticEvent } from "react";

type itemProps = {
  id: number;
  imageUrl: string;
  title: string;
  category: string;
  nickname: string;
  acquiredAt: Date;
};

const MainListItem = ({
  id,
  imageUrl,
  title,
  category,
  nickname,
  acquiredAt,
}: itemProps) => {
  const requestDetail = (id: number) => console.log(id + "번 디테일 요청");
  const requestScrap = (id: number) => console.log(id + "번 스크랩 요청");
  const requestOriginalImage = (url: string) =>
    console.log(url + " 원본 이미지 요청");

  return (
    <>
      <div className="flex flex-row  border-y">
        <div>
          <img
            className="w-[80px] h-[80px] m-[10px] cursor-pointer"
            src={imageUrl}
            onClick={() => requestOriginalImage(imageUrl)}
            alt="no Image"
          ></img>
        </div>
        <div
          className="h-[100px] flex flex-row cursor-pointer"
          onClick={() => requestDetail(id)}
        >
          <div className="w-[180px]">
            <Text
              children={title}
              className="text-sm font-bold mt-[10px] m-[5px]"
            />
            <Text children={category} className="text-xs m-[5px]" />
            <Text children={nickname} className="text-xs ml-[5px] mt-[20px]" />
          </div>
          <div className="w-[80px]">
            <CustomButton
              className="w-auto ml-[36px] mt-[10px]"
              onClick={(e?: SyntheticEvent) => {
                e?.stopPropagation();
                requestScrap(id);
              }}
            >
              <FavoriteBorderIcon />
            </CustomButton>
            <Text
              children={"습득일: " + acquiredAt.toLocaleDateString()}
              className="text-xs mt-[20px]"
            />
          </div>
        </div>
      </div>
    </>
  );
};

export default MainListItem;
