import SendOutlinedIcon from "@mui/icons-material/SendOutlined";
import FavoriteIcon from "@mui/icons-material/Favorite";
import FavoriteBorderOutlinedIcon from "@mui/icons-material/FavoriteBorderOutlined";
import ColorLensIcon from "@mui/icons-material/ColorLens";
import { Breadcrumb, CustomButton, KakaoMap, Text } from "@/shared";
import { Label } from "flowbite-react";
// import { SlidingWindow } from "@/widgets";

const foundItemDetail = () => {
  const mainCategory = "지갑";
  const subCategory = "카드지갑";
  const userId = "아이디";
  const imageUrl = "../images/Findear-ls.png";
  const color = "빨간색";
  const title = "타이틀입니다.";
  const description = "내용입니다.";
  const place = "보관장소입니다.";
  const role = "NORMAL";
  const isScrapped = false;
  const returnState = "READY";
  const collapseSlidingWindow = () => {};

  const checkReturnState = (returnState: string) => {
    switch (returnState) {
      case "READY":
        return (
          <CustomButton
            className="rounded-md w-[320px] h-[60px] bg-black text-white"
            onClick={() => collapseSlidingWindow()}
          >
            <p>인계하기</p>
          </CustomButton>
        );
      case "AUTHENTICATIONING":
        return (
          <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706Yellow text-black">
            <p>인증 중</p>
          </CustomButton>
        );
      case "AUTHENTICATED":
        return (
          <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706CheryBlue text-white">
            <p>인증 완료</p>
          </CustomButton>
        );
      case "RETURNED":
        return (
          <CustomButton className="rounded-md w-[320px] h-[60px] bg-A706Green text-white">
            <p>인계 완료</p>
          </CustomButton>
        );
    }
  };

  const showSendButton = (role: string) => {
    if (role === "NORMAL") {
      return (
        <CustomButton className="main-nav-button">
          <>
            <SendOutlinedIcon />
            쪽지 보내기
          </>
        </CustomButton>
      );
    }
    return <></>;
  };

  const showScrapbutton = (isScrapped: boolean) => {
    if (isScrapped) {
      return (
        <CustomButton className="main-nav-button">
          <>
            <FavoriteBorderOutlinedIcon />
            <p>스크랩하기</p>
          </>
        </CustomButton>
      );
    } else {
      return (
        <CustomButton className="main-nav-button">
          <>
            <FavoriteIcon />
            <p>스크랩 완료</p>
          </>
        </CustomButton>
      );
    }
  };

  return (
    <>
      <div className="flex flex-col justify-center items-center p-[40px]">
        <div className="flex flex-row justify-between w-[340px] mb-10">
          <Breadcrumb>{[mainCategory, subCategory]}</Breadcrumb>
          <Text>{"습득자: " + userId}</Text>
        </div>
        <img
          src={imageUrl}
          alt="이미지가 없습니다."
          className="w-[340px] h-[340px] self-center"
        ></img>
        <div className="mt-10">
          <Label color="secondary" value="제목" />
          <Text children={title} className="w-[340px] text-lg font-bold" />
          <div className="h-[1px] bg-A706DarkGrey2"></div>
        </div>
        <div className="w-[340px] mt-10 flex justify-start">
          <ColorLensIcon />
          <Text>{color}</Text>
        </div>
        <div className="w-[340px] mt-10">
          <Label color="secondary" value="설명" />
          <div className="bg-white shadow-md min-h-[50px] p-5 rounded-md">
            {description}
          </div>
        </div>
        <div className="w-[340px] mt-10">
          <Label color="secondary" value="습득장소" />
          <KakaoMap className="h-[340px] self-center rounded-md shadow-md"></KakaoMap>
        </div>
        <div className="w-[340px] mt-10">
          <Label color="secondary" value="보관장소" />
          <Text children={place} className="w-[340px] text-lg font-bold" />
          <div className="h-[1px] bg-A706DarkGrey2"></div>
        </div>
        <div className="w-[340px] mt-10 flex flex-row justify-around">
          {showSendButton(role)}
          {role === "NORMAL"
            ? showScrapbutton(isScrapped)
            : checkReturnState(returnState)}
        </div>
        {/* {role === "MANAGER" ? <SlidingWindow /> : <></>} */}
      </div>
    </>
  );
};

export default foundItemDetail;
