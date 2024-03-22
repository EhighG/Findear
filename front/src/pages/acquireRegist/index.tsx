import React, { useRef } from "react";
import { Text } from "@/shared";
import { GoImage } from "react-icons/go";
import { useState } from "react";
import { KakaoMap } from "@/shared";
import { TextInput } from "flowbite-react";
const AcquireRegist = () => {
  const [images, setImages] = useState<File[]>([]);
  const imageRef = useRef<HTMLInputElement>(null);
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setImages([...images, ...Array.from(e.target.files as FileList)]);
  };

  return (
    <div className="flex flex-col flex-1 mx-[10px]">
      <Text className="text-[28px] font-bold">습득물 등록하기</Text>
      <div className="flex gap-[10px]">
        <div>
          <label htmlFor="image">
            <div className="flex flex-col items-center">
              <GoImage size="80px" />
              <Text className="text-[28px]">{images.length.toString()}/6</Text>
            </div>
          </label>
          <input
            id="image"
            ref={imageRef}
            title="image"
            type="file"
            multiple
            className="hidden"
            onChange={handleChange}
          ></input>
        </div>
        {/* 올린 이미지 미리 보기 */}
        {/* 이미지가 만약 많아지면 스크롤이 생기게 스타일좀 해줘 */}
        <div className="flex overflow-x-scroll gap-[10px]">
          {images.map((image, index) => {
            return (
              <div key={index} className="flex flex-col items-center">
                <img
                  src={URL.createObjectURL(image)}
                  alt="image"
                  className="min-w-[80px] h-[80px]"
                />
                <button
                  onClick={() => {
                    setImages(images.filter((_, i) => i !== index));
                  }}
                >
                  삭제
                </button>
              </div>
            );
          })}
        </div>
      </div>
      <div className="flex flex-col items-center">
        <TextInput
          id="address"
          placeholder="사용자 주소"
          readOnly
          value="사용자주소"
          className="w-full"
          required
        />
        <TextInput
          id="agencyName"
          placeholder="시설명을 입력해주세요"
          value="시설명"
          className="w-full"
          required
        />
        <KakaoMap
          className="size-[340px]"
          //   keyword={address}
          //   setPosition={setPosition}
        />
      </div>
    </div>
  );
};

export default AcquireRegist;
