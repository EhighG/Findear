import ColorLensIcon from "@mui/icons-material/ColorLens";
import { Breadcrumb, KakaoMap, Text } from "@/shared";
import { Label, TextInput, Textarea } from "flowbite-react";

const foundItemWrite = () => {
  const mainCategory = "지갑";
  const subCategory = "카드지갑";
  const userId = "아이디";
  const imageUrl = "../images/Findear-ls.png";
  const color = "빨간색";
  return (
    <>
      <div className="flex flex-col justify-center items-center p-[40px]">
        <div className="flex flex-row justify-between w-[340px] mb-5">
          <Breadcrumb>{[mainCategory, subCategory]}</Breadcrumb>
          <Text>{"습득자: " + userId}</Text>
        </div>
        <img
          src={imageUrl}
          alt="이미지가 없습니다."
          className="w-[340px] h-[340px] self-center"
        ></img>
        <div className="mt-5">
          <Label color="secondary" value="타이틀" />
          <TextInput className="w-[340px]" placeholder="타이틀"></TextInput>
        </div>
        <div className="w-[340px] mt-5 flex justify-start">
          <ColorLensIcon />
          <Text>{color}</Text>
        </div>
        <div className="w-[340px] mt-5">
          <Label color="secondary" value="설명" />
          <Textarea></Textarea>
        </div>
        <KakaoMap className="w-[340px] h-[340px] my-5 self-center rounded-md"></KakaoMap>
        <div className="w-[340px] mt-5">
          <Label color="secondary" value="보관장소" />
          <TextInput placeholder="보관 장소"></TextInput>
        </div>
        <div></div>
      </div>
    </>
  );
};

export default foundItemWrite;
