import React, { useRef, useEffect, useContext } from "react";
import { CustomButton, StateContext, Text } from "@/shared";
import { GoImage } from "react-icons/go";
import { useState } from "react";
import { KakaoMap } from "@/shared";
import { TextInput } from "flowbite-react";
import AWS from "aws-sdk";
import { useGenerateHexCode } from "@/shared";

const AcquireRegist = () => {
  const [images, setImages] = useState<File[]>([]);
  const [urlList, setUrlList] = useState<string[]>([]);
  const imageRef = useRef<HTMLInputElement>(null);
  const { setHeaderTitle } = useContext(StateContext);

  AWS.config.update({
    accessKeyId: import.meta.env.VITE_S3_ACCESS_KEY,
    secretAccessKey: import.meta.env.VITE_S3_SECRET_ACCESS_KEY,
  });

  const myBucket = new AWS.S3({
    params: { Bucket: "findearbucket" },
    region: "ap-northeast-2",
  });

  const uploadFile = async () => {
    try {
      // 모든 이미지의 업로드를 기다리기 위한 Promise 배열 생성
      const uploadPromises = images.map((image) => {
        return new Promise((resolve, reject) => {
          const imageName = useGenerateHexCode();
          const params = {
            Body: image,
            Bucket: "findearbucket",
            Key: imageName,
          };

          myBucket.putObject(params, (err, data) => {
            if (err) {
              console.error("이미지 업로드 중에 오류가 발생했습니다.", err);
              setUrlList([]);
              setImages([]);
              reject(err); // 에러 발생 시 reject 호출
            } else {
              const url = myBucket.getSignedUrl("getObject", {
                Key: params.Key,
              });
              const imageUrl = url.split("?")[0];
              urlList.push(imageUrl);
              resolve(data); // 성공 시 resolve 호출
            }
          });
        });
      });

      // 모든 이미지 업로드가 완료될 때까지 기다림
      await Promise.all(uploadPromises).then(() => {
        registItem();
      });

      // 이미지 업로드가 완료되면 registItem 함수 호출
    } catch (error) {
      console.error("이미지 업로드 중에 오류가 발생했습니다.", error);
    }
  };

  const registItem = () => {
    if (urlList.length === 0) return;

    // 서버에 이미지 URL과 함께 데이터 전송
    console.log("등록 완료");
    // 등록 완료되면 lists로 이동
  };

  const handleUpload = async () => {
    uploadFile(); // AWS S3에 이미지 업로드
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files) return;
    setImages([...e.target.files]);
  };

  useEffect(() => {
    setHeaderTitle("습득물 등록");
    return () => setHeaderTitle("");
  }, []);

  return (
    <div className="flex flex-col flex-1 mx-[10px] ">
      <Text className="text-[28px] font-bold">습득물 등록하기</Text>
      <div className="flex gap-[10px]">
        <div>
          <label htmlFor="image">
            <div className="flex flex-col items-center border border-A706Grey2 rounded-lg size-[50px] cursor-pointer">
              <GoImage size="100" />
              <Text>{images.length.toString()}/6</Text>
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
        <div className="flex overflow-x-scroll gap-[10px]">
          {images.map((image, index) => {
            return (
              <div key={index} className="flex flex-col items-center">
                <img
                  src={URL.createObjectURL(image)}
                  alt="image"
                  className="min-w-[80px] h-[80px]"
                />
                <CustomButton
                  className="text-xl"
                  onClick={() => {
                    setImages(images.filter((_, i) => i !== index));
                  }}
                >
                  삭제
                </CustomButton>
              </div>
            );
          })}
        </div>
      </div>
      <div className="flex flex-col items-center mx-[10px] mt-[20px] gap-[10px]">
        <TextInput
          id="address"
          placeholder="사용자 주소"
          readOnly
          value="사용자가 등록한 주소"
          className="w-full max-w-sm"
          required
        />
        <TextInput
          id="agencyName"
          placeholder="시설명"
          readOnly
          value="사용자가 등록한 시설명"
          className="w-full max-w-sm"
          required
        />
        <KakaoMap
          className="size-[340px]"
          //   keyword={address}
          //   setPosition={setPosition}
        />
        <CustomButton
          className="menubtn my-[10px]"
          onClick={() => handleUpload()}
        >
          등록하기
        </CustomButton>
      </div>
    </div>
  );
};

export default AcquireRegist;
